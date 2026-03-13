package com.college.icrs.integration;

import com.college.icrs.model.Role;
import com.college.icrs.model.User;
import com.college.icrs.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.function.Predicate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class GrievanceApiIntegrationTestSupport {

    protected static final String LOGIN_EMAIL = "2022bit052@sggs.ac.in";
    protected static final String LOGIN_PASSWORD = "Test@12345";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String loginAndGetBearerToken() throws Exception {
        ensureAiSystemUser();
        ensureStudentAccountForLogin();

        String loginPayload = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(LOGIN_EMAIL, LOGIN_PASSWORD);

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(login.getResponse().getContentAsString());
        String token = loginJson.path("token").asText(null);
        Assertions.assertNotNull(token, "Login token should not be null");
        Assertions.assertFalse(token.isBlank(), "Login token should not be blank");
        return token;
    }

    protected long getCatalogCategoryIdByName(String categoryName) throws Exception {
        JsonNode categories = getCategories();
        for (JsonNode category : categories) {
            if (categoryName.equalsIgnoreCase(category.path("name").asText())) {
                return category.path("id").asLong();
            }
        }

        throw new IllegalArgumentException("Catalog category not found: " + categoryName);
    }

    protected JsonNode getCategories() throws Exception {
        MvcResult result = mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode submitGrievance(String token, Long categoryId, String title, String description) throws Exception {
        String payload = """
                {
                  "title": "%s",
                  "description": "%s",
                  "categoryId": %d,
                  "registrationNumber": "2022BIT052"
                }
                """.formatted(title, description, categoryId);

        MvcResult result = mockMvc.perform(post("/grievances")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode getMyGrievances(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/grievances/student/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode getComments(String token, long grievanceId) throws Exception {
        MvcResult result = mockMvc.perform(get("/grievances/{id}/comments", grievanceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected JsonNode waitForMyGrievance(String token, long grievanceId, Predicate<JsonNode> condition) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        JsonNode lastSeen = null;

        while (System.currentTimeMillis() < deadline) {
            JsonNode grievances = getMyGrievances(token);
            for (JsonNode grievance : grievances) {
                if (grievance.path("id").asLong() == grievanceId) {
                    lastSeen = grievance;
                    if (condition.test(grievance)) {
                        return grievance;
                    }
                }
            }
            Thread.sleep(200);
        }

        if (lastSeen != null) {
            return lastSeen;
        }

        throw new IllegalStateException("Timed out waiting for grievance " + grievanceId);
    }

    private void ensureStudentAccountForLogin() {
        User user = userRepository.findByEmail(LOGIN_EMAIL).orElseGet(User::new);
        user.setUsername("Integration Student");
        user.setEmail(LOGIN_EMAIL);
        user.setPassword(passwordEncoder.encode(LOGIN_PASSWORD));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        user.setDepartment("CSE");
        user.setStudentId("2022BIT052");
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
    }

    private void ensureAiSystemUser() {
        User aiSystem = userRepository.findByEmail("ai.system@icrs.local").orElseGet(User::new);
        aiSystem.setUsername("AI System");
        aiSystem.setEmail("ai.system@icrs.local");
        aiSystem.setPassword(passwordEncoder.encode("ai-system-disabled-login"));
        aiSystem.setRole(Role.ADMIN);
        aiSystem.setEnabled(true);
        aiSystem.setDepartment("SYSTEM");
        aiSystem.setStudentId("AI_SYSTEM");
        aiSystem.setVerificationCode(null);
        aiSystem.setVerificationCodeExpiresAt(null);
        userRepository.save(aiSystem);
    }
}
