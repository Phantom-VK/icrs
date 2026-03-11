package com.college.icrs.integration;

import com.college.icrs.model.Category;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "icrs.ai.enabled=false",
        "notifications.enabled=false"
})
@AutoConfigureMockMvc
class GrievanceApiAiDisabledIT extends GrievanceApiIntegrationTestSupport {

    @MockBean
    private ChatModel chatModel;

    @Autowired
    private com.college.icrs.repository.GrievanceRepository grievanceRepository;

    @Test
    void shouldCreateGrievanceWithAiFieldsUnsetWhenAiDisabled() throws Exception {
        String token = loginAndGetBearerToken();
        Category category = createCategory(false, false, "AI-DISABLED");

        JsonNode grievance = submitGrievance(
                token,
                category.getId(),
                "Hostel water issue",
                "Water is unavailable in hostel from last night"
        );

        assertFalse(grievance.path("aiResolved").asBoolean(true));
        assertTrue(grievance.path("aiTitle").isNull() || grievance.path("aiTitle").asText("").isBlank());
        assertTrue(grievance.path("sentiment").isNull() || grievance.path("sentiment").asText("").isBlank());
        assertTrue(grievance.path("aiDecisionAt").isNull() || grievance.path("aiDecisionAt").asText("").isBlank());
    }

    @Test
    void shouldAllowLoginCategoriesAndMyGrievancesFlow() throws Exception {
        String token = loginAndGetBearerToken();

        MvcResult categoriesResult = mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode categories = objectMapper.readTree(categoriesResult.getResponse().getContentAsString());
        assertTrue(categories.isArray());

        Category category = createCategory(false, false, "FLOW-CHECK");
        submitGrievance(token, category.getId(), "Library issue", "Need additional reading room timings.");

        JsonNode myGrievances = getMyGrievances(token);
        assertTrue(myGrievances.isArray());
        assertTrue(myGrievances.size() >= 1);

        long count = grievanceRepository.findByStudentId(
                userRepository.findByEmail(LOGIN_EMAIL).orElseThrow().getId()
        ).size();
        assertTrue(count >= 1);
    }
}
