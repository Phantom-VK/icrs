package com.college.icrs.ai.knowledge;

import com.college.icrs.model.Grievance;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ResolutionGuidanceService {

    private static final String KNOWLEDGE_BASE_PATH = "knowledge/resolution-guidance.json";

    private final ResolutionGuidanceKnowledgeBase knowledgeBase;

    public ResolutionGuidanceService(ObjectMapper objectMapper) {
        this.knowledgeBase = loadKnowledgeBase(objectMapper);
    }

    public String buildContext(Grievance grievance) {
        if (grievance == null || grievance.getCategory() == null || !StringUtils.hasText(grievance.getCategory().getName())) {
            return "";
        }

        Optional<ResolutionGuidanceCategory> category = findCategory(grievance.getCategory().getName());
        if (category.isEmpty()) {
            return "";
        }

        ResolutionGuidanceEntry categoryGuidance = category.get().getDefaultGuidance();
        ResolutionGuidanceEntry subcategoryGuidance = findSubcategoryGuidance(
                category.get(),
                grievance.getSubcategory() != null ? grievance.getSubcategory().getName() : null
        ).orElse(null);

        return formatContext(
                grievance.getCategory().getName(),
                grievance.getSubcategory() != null ? grievance.getSubcategory().getName() : null,
                categoryGuidance,
                subcategoryGuidance
        );
    }

    private ResolutionGuidanceKnowledgeBase loadKnowledgeBase(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(KNOWLEDGE_BASE_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, ResolutionGuidanceKnowledgeBase.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load resolution guidance knowledge base", ex);
        }
    }

    private Optional<ResolutionGuidanceCategory> findCategory(String categoryName) {
        return safeList(knowledgeBase.getCategories()).stream()
                .filter(entry -> matches(entry.getCategory(), categoryName))
                .findFirst();
    }

    private Optional<ResolutionGuidanceEntry> findSubcategoryGuidance(
            ResolutionGuidanceCategory category,
            String subcategoryName
    ) {
        if (!StringUtils.hasText(subcategoryName)) {
            return Optional.empty();
        }
        return safeSubcategories(category.getSubcategories()).stream()
                .filter(entry -> matches(entry.getSubcategory(), subcategoryName))
                .map(ResolutionGuidanceSubcategory::getGuidance)
                .findFirst();
    }

    private String formatContext(
            String categoryName,
            String subcategoryName,
            ResolutionGuidanceEntry categoryGuidance,
            ResolutionGuidanceEntry subcategoryGuidance
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("- category: ").append(safe(categoryName)).append('\n');
        builder.append("- subcategory: ").append(safe(subcategoryName, "UNKNOWN")).append('\n');
        appendEntry(builder, "category guidance", categoryGuidance);
        appendEntry(builder, "subcategory guidance", subcategoryGuidance);
        return builder.toString().trim();
    }

    private void appendEntry(StringBuilder builder, String sectionName, ResolutionGuidanceEntry entry) {
        if (entry == null) {
            return;
        }
        builder.append(sectionName).append(":\n");
        appendLine(builder, "officeName", entry.getOfficeName());
        appendLine(builder, "facultyOrDesk", entry.getFacultyOrDesk());
        appendLine(builder, "responsibility", entry.getResponsibility());
        appendLine(builder, "building", entry.getBuilding());
        appendLine(builder, "floor", entry.getFloor());
        appendLine(builder, "room", entry.getRoom());
        appendLine(builder, "openHours", entry.getOpenHours());
        appendLine(builder, "contactEmail", entry.getContactEmail());
        appendLine(builder, "contactPhone", entry.getContactPhone());
        appendLine(builder, "studentAction", entry.getStudentAction());
        appendLine(builder, "escalationNote", entry.getEscalationNote());
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append("- ").append(label).append(": ").append(value.trim()).append('\n');
    }

    private boolean matches(String left, String right) {
        return StringUtils.hasText(left)
                && StringUtils.hasText(right)
                && left.trim().toLowerCase(Locale.ROOT).equals(right.trim().toLowerCase(Locale.ROOT));
    }

    private List<ResolutionGuidanceCategory> safeList(List<ResolutionGuidanceCategory> value) {
        return value != null ? value : List.of();
    }

    private List<ResolutionGuidanceSubcategory> safeSubcategories(List<ResolutionGuidanceSubcategory> value) {
        return value != null ? value : List.of();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
