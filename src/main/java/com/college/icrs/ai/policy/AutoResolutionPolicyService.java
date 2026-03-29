package com.college.icrs.ai.policy;

import com.college.icrs.model.Grievance;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

@Service
public class AutoResolutionPolicyService {

    private static final String KNOWLEDGE_BASE_PATH = "knowledge/auto-resolution-policy.json";

    private final AutoResolutionPolicyKnowledgeBase knowledgeBase;

    public AutoResolutionPolicyService(ObjectMapper objectMapper) {
        this.knowledgeBase = loadKnowledgeBase(objectMapper);
    }

    public boolean supportsRoutineAutoResolve(Grievance grievance) {
        if (grievance == null || grievance.getCategory() == null || grievance.getSubcategory() == null) {
            return false;
        }

        String category = normalize(grievance.getCategory().getName());
        String subcategory = normalize(grievance.getSubcategory().getName());
        String grievanceText = normalize("%s %s".formatted(grievance.getTitle(), grievance.getDescription()));

        return safeRules(knowledgeBase.getRoutineRules()).stream()
                .filter(rule -> matches(rule.getCategory(), category) && matches(rule.getSubcategory(), subcategory))
                .anyMatch(rule -> safePhrases(rule.getMatchAnyPhrases()).stream()
                        .map(this::normalize)
                        .anyMatch(phrase -> StringUtils.hasText(phrase) && grievanceText.contains(phrase)));
    }

    private AutoResolutionPolicyKnowledgeBase loadKnowledgeBase(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(KNOWLEDGE_BASE_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, AutoResolutionPolicyKnowledgeBase.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load auto-resolution policy knowledge base", ex);
        }
    }

    private boolean matches(String configuredValue, String normalizedGrievanceValue) {
        return StringUtils.hasText(configuredValue)
                && StringUtils.hasText(normalizedGrievanceValue)
                && normalize(configuredValue).equals(normalizedGrievanceValue);
    }

    private List<RoutineAutoResolveRule> safeRules(List<RoutineAutoResolveRule> rules) {
        return rules != null ? rules : List.of();
    }

    private List<String> safePhrases(List<String> phrases) {
        return phrases != null ? phrases : List.of();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('&', ' ')
                .replace('/', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");
    }
}
