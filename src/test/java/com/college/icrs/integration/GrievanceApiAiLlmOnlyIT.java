package com.college.icrs.integration;

import com.college.icrs.model.Status;
import com.college.icrs.repository.StatusHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "icrs.ai.enabled=true",
        "icrs.ai.sentiment.enabled=false",
        "icrs.ai.auto-resolve-confidence-threshold=0.20",
        "notifications.enabled=false"
})
@AutoConfigureMockMvc
class GrievanceApiAiLlmOnlyIT extends GrievanceApiIntegrationTestSupport {

    @MockBean
    private ChatModel chatModel;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Test
    void shouldCreateGrievanceWhenLlmFailsWithoutBreakingFlow() throws Exception {
        Mockito.when(chatModel.chat(Mockito.anyString())).thenReturn("not-json");

        String token = loginAndGetBearerToken();
        long categoryId = getCatalogCategoryIdByName("Finance & Scholarships");
        JsonNode grievance = submitGrievance(
                token,
                categoryId,
                "Mess food concern",
                "Food quality is inconsistent and needs review"
        );

        assertEquals("IN_PROGRESS", grievance.path("status").asText());
        assertFalse(grievance.path("aiResolved").asBoolean(true));
        assertTrue(grievance.path("aiDecisionAt").isNull() || grievance.path("aiDecisionAt").asText().isBlank());
    }

    @Test
    void shouldAutoResolveAndCreateAiSystemCommentForNonSensitiveGrievance() throws Exception {
        Mockito.when(chatModel.chat(Mockito.anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0, String.class);
            if (prompt.contains("grievance triage classifier")) {
                return """
                        {
                          "priority":"LOW",
                          "aiTitle":"WiFi connectivity issue",
                          "summary":"Network disconnects intermittently in hostel block",
                          "confidence":0.95
                        }
                        """;
            }
            if (prompt.contains("AI grievance resolver")) {
                return """
                        {
                          "autoResolve":true,
                          "resolutionText":"Please reset your WiFi profile and reconnect. IT support has been notified.",
                          "internalComment":"Standard connectivity issue with a known fix pattern.",
                          "confidence":0.96
                        }
                        """;
            }
            return """
                    {"autoResolve":false,"resolutionText":"","internalComment":"Fallback","confidence":0.4}
                    """;
        });

        String token = loginAndGetBearerToken();
        long categoryId = getCatalogCategoryIdByName("IT Support");

        JsonNode grievance = submitGrievance(
                token,
                categoryId,
                "WiFi access issue",
                "My hostel WiFi disconnects every 10 minutes."
        );

        long grievanceId = grievance.path("id").asLong();
        JsonNode updatedGrievance = waitForMyGrievance(
                token,
                grievanceId,
                g -> "RESOLVED".equals(g.path("status").asText()) && g.path("aiResolved").asBoolean(false)
        );

        assertEquals("RESOLVED", updatedGrievance.path("status").asText());
        assertTrue(updatedGrievance.path("aiResolved").asBoolean(false));
        assertTrue(updatedGrievance.path("aiResolutionText").asText("").contains("WiFi"));

        JsonNode comments = getComments(token, grievanceId);
        boolean hasAiComment = false;
        for (JsonNode comment : comments) {
            if (comment.path("body").asText("").contains("[AI Auto-Resolution]")) {
                hasAiComment = true;
                break;
            }
        }
        assertTrue(hasAiComment, "Expected AI system auto-resolution comment");

        List<com.college.icrs.model.StatusHistory> statusHistory =
                statusHistoryRepository.findByGrievanceIdOrderByChangedAtDesc(grievanceId);
        assertFalse(statusHistory.isEmpty());
        assertEquals(Status.RESOLVED, statusHistory.get(0).getToStatus());
        assertTrue(statusHistory.get(0).getReason() != null
                && statusHistory.get(0).getReason().contains("Resolved by AI"));
    }
}
