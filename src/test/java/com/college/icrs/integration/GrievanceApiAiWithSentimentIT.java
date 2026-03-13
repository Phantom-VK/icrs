package com.college.icrs.integration;

import com.college.icrs.ai.service.SentimentAnalysisService;
import com.college.icrs.model.Sentiment;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "icrs.ai.enabled=true",
        "icrs.ai.sentiment.enabled=true",
        "icrs.ai.auto-resolve-confidence-threshold=0.20",
        "notifications.enabled=false"
})
@AutoConfigureMockMvc
class GrievanceApiAiWithSentimentIT extends GrievanceApiIntegrationTestSupport {

    @MockBean
    private ChatModel chatModel;

    @MockBean
    private SentimentAnalysisService sentimentAnalysisService;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Test
    void shouldStoreSentimentFromMlServiceInGrievanceMetadata() throws Exception {
        Mockito.when(sentimentAnalysisService.analyze(Mockito.anyString()))
                .thenReturn(new SentimentAnalysisService.SentimentDecision(
                        Sentiment.NEGATIVE,
                        0.91,
                        "mock-sentiment-model"
                ));

        Mockito.when(chatModel.chat(Mockito.anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0, String.class);
            if (prompt.contains("grievance triage classifier")) {
                return """
                        {
                          "priority":"MEDIUM",
                          "aiTitle":"Hostel washroom maintenance request",
                          "summary":"Repeated cleaning issues reported",
                          "confidence":0.90
                        }
                        """;
            }
            return """
                    {
                      "autoResolve":false,
                      "resolutionText":"",
                      "internalComment":"Requires hostel office review.",
                      "confidence":0.70
                    }
                    """;
        });

        String token = loginAndGetBearerToken();
        long categoryId = getCatalogCategoryIdByName("Hostel & Accommodation");

        JsonNode grievance = submitGrievance(
                token,
                categoryId,
                "Hostel hygiene concern",
                "Washroom cleaning has been delayed multiple times this week."
        );

        JsonNode updatedGrievance = waitForMyGrievance(
                token,
                grievance.path("id").asLong(),
                g -> g.path("aiDecisionAt").asText("").length() > 0
        );

        assertTrue("NEGATIVE".equals(updatedGrievance.path("sentiment").asText())
                || "VERY_NEGATIVE".equals(updatedGrievance.path("sentiment").asText()));
        assertTrue(updatedGrievance.path("aiModelName").asText("").contains("sentiment:mock-sentiment-model"));
        assertFalse(updatedGrievance.path("aiResolved").asBoolean(true));
        assertTrue(updatedGrievance.path("aiDecisionAt").asText("").length() > 0);
    }

    @Test
    void shouldNeverAutoResolveSensitiveCategoryEvenIfLlmRequestsIt() throws Exception {
        Mockito.when(sentimentAnalysisService.analyze(Mockito.anyString()))
                .thenReturn(new SentimentAnalysisService.SentimentDecision(
                        Sentiment.NEGATIVE,
                        0.95,
                        "mock-sentiment-model"
                ));

        Mockito.when(chatModel.chat(Mockito.anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0, String.class);
            if (prompt.contains("grievance triage classifier")) {
                return """
                        {
                          "priority":"HIGH",
                          "aiTitle":"Sensitive grievance summary",
                          "summary":"Sensitive case requires strict handling",
                          "confidence":0.96
                        }
                        """;
            }
            return """
                    {
                      "autoResolve":true,
                      "resolutionText":"Auto response from AI",
                      "internalComment":"LLM suggested auto resolution",
                      "confidence":0.97
                    }
                    """;
        });

        String token = loginAndGetBearerToken();
        long categoryId = getCatalogCategoryIdByName("Harassment / PoSH");

        JsonNode grievance = submitGrievance(
                token,
                categoryId,
                "Sensitive issue",
                "This is a sensitive grievance that must not be auto-resolved."
        );

        long grievanceId = grievance.path("id").asLong();
        JsonNode updatedGrievance = waitForMyGrievance(
                token,
                grievanceId,
                g -> g.path("aiDecisionAt").asText("").length() > 0
        );

        assertFalse("RESOLVED".equals(updatedGrievance.path("status").asText()));
        assertFalse(updatedGrievance.path("aiResolved").asBoolean(true));
        assertTrue(updatedGrievance.path("aiResolutionComment").asText("").contains("confidence="));

        List<com.college.icrs.model.StatusHistory> history =
                statusHistoryRepository.findByGrievanceIdOrderByChangedAtDesc(grievanceId);
        boolean hasResolvedByAi = history.stream().anyMatch(h ->
                h.getReason() != null && h.getReason().contains("Resolved by AI"));
        assertFalse(hasResolvedByAi);
    }
}
