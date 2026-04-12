package com.college.icrs.evaluation;

import com.college.icrs.dto.CategoryResponseDTO;
import com.college.icrs.service.CategoryCatalogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OperationalEvaluationDatasetValidator {

    private final ObjectMapper objectMapper;
    private final CategoryCatalogService categoryCatalogService;

    public OperationalEvaluationDatasets validate(Path historicalFile, Path liveFile) {
        CatalogIndex catalogIndex = CatalogIndex.from(categoryCatalogService.listCatalog());
        List<String> errors = new ArrayList<>();

        List<OperationalEvaluationHistoricalCase> historicalCases = validateHistoricalCases(historicalFile, catalogIndex, errors);
        List<OperationalEvaluationLiveCase> liveCases = validateLiveCases(liveFile, catalogIndex, errors);

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Operational evaluation dataset validation failed:\n- "
                    + String.join("\n- ", errors));
        }

        return new OperationalEvaluationDatasets(historicalCases, liveCases);
    }

    private List<OperationalEvaluationHistoricalCase> validateHistoricalCases(
            Path file,
            CatalogIndex catalogIndex,
            List<String> errors
    ) {
        JsonNode cases = readArray(file, "historicalCases", "grievances");
        Set<String> seenIds = new HashSet<>();
        List<OperationalEvaluationHistoricalCase> validated = new ArrayList<>();

        int index = 0;
        for (JsonNode node : cases) {
            String documentId = firstNonBlank(text(node, "documentId"), text(node, "id"));
            String title = requiredText(node, "title");
            String description = requiredText(node, "description");
            String category = requiredText(node, "category");
            String subcategory = requiredText(node, "subcategory");
            String registrationNumber = requiredText(node, "registrationNumber");

            String location = "%s[%d]".formatted(file, index++);
            if (!StringUtils.hasText(documentId)) {
                errors.add(location + " missing required field: documentId");
                continue;
            }
            if (!seenIds.add(normalize(documentId))) {
                errors.add(location + " duplicate documentId: " + documentId);
            }
            validateTextFields(location, title, description, registrationNumber, errors);
            validateCategorySelection(location, category, subcategory, catalogIndex, errors);

            validated.add(new OperationalEvaluationHistoricalCase(
                    documentId.trim(),
                    safe(title),
                    safe(description),
                    safe(category),
                    safe(subcategory),
                    safe(registrationNumber),
                    text(node, "priority"),
                    text(node, "sentiment"),
                    firstNonBlank(text(node, "resolutionText"), text(node, "aiResolutionText")),
                    catalogIndex.isSensitive(category)
            ));
        }

        return validated;
    }

    private List<OperationalEvaluationLiveCase> validateLiveCases(
            Path file,
            CatalogIndex catalogIndex,
            List<String> errors
    ) {
        JsonNode cases = readArray(file, "liveCases", "grievances");
        Set<String> seenIds = new HashSet<>();
        List<OperationalEvaluationLiveCase> validated = new ArrayList<>();

        int index = 0;
        for (JsonNode node : cases) {
            String caseId = firstNonBlank(text(node, "caseId"), text(node, "documentId"), text(node, "id"));
            String title = requiredText(node, "title");
            String description = requiredText(node, "description");
            String category = requiredText(node, "category");
            String subcategory = requiredText(node, "subcategory");
            String registrationNumber = requiredText(node, "registrationNumber");

            String location = "%s[%d]".formatted(file, index++);
            if (!StringUtils.hasText(caseId)) {
                errors.add(location + " missing required stable case id (caseId/documentId/id)");
                continue;
            }
            if (!seenIds.add(normalize(caseId))) {
                errors.add(location + " duplicate case id: " + caseId);
            }
            validateTextFields(location, title, description, registrationNumber, errors);
            validateCategorySelection(location, category, subcategory, catalogIndex, errors);

            validated.add(new OperationalEvaluationLiveCase(
                    caseId.trim(),
                    safe(title),
                    safe(description),
                    safe(category),
                    safe(subcategory),
                    safe(registrationNumber),
                    catalogIndex.isSensitive(category)
            ));
        }

        return validated;
    }

    private JsonNode readArray(Path file, String... candidateFields) {
        if (file == null || !Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Evaluation dataset file not found: " + file);
        }

        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            if (root.isArray()) {
                return root;
            }
            for (String field : candidateFields) {
                JsonNode value = root.path(field);
                if (value.isArray()) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Expected a JSON array or one of these array fields in "
                    + file + ": " + String.join(", ", candidateFields));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read evaluation dataset file: " + file, ex);
        }
    }

    private void validateTextFields(
            String location,
            String title,
            String description,
            String registrationNumber,
            List<String> errors
    ) {
        if (!StringUtils.hasText(title)) {
            errors.add(location + " title must not be blank");
        }
        if (!StringUtils.hasText(description)) {
            errors.add(location + " description must not be blank");
        }
        if (!StringUtils.hasText(registrationNumber)) {
            errors.add(location + " registrationNumber must not be blank");
        }
    }

    private void validateCategorySelection(
            String location,
            String category,
            String subcategory,
            CatalogIndex catalogIndex,
            List<String> errors
    ) {
        if (!catalogIndex.containsCategory(category)) {
            errors.add(location + " unknown category: " + category);
            return;
        }
        if (!catalogIndex.containsSubcategory(category, subcategory)) {
            errors.add(location + " unknown subcategory '" + subcategory + "' for category '" + category + "'");
        }
    }

    private String requiredText(JsonNode node, String fieldName) {
        return text(node, fieldName);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private String safe(String value) {
        return value == null ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static final class CatalogIndex {

        private final Map<String, CategoryResponseDTO> categoriesByName;
        private final Map<String, Set<String>> subcategoriesByCategory;

        private CatalogIndex(Map<String, CategoryResponseDTO> categoriesByName, Map<String, Set<String>> subcategoriesByCategory) {
            this.categoriesByName = categoriesByName;
            this.subcategoriesByCategory = subcategoriesByCategory;
        }

        static CatalogIndex from(List<CategoryResponseDTO> categories) {
            Map<String, CategoryResponseDTO> categoriesByName = new HashMap<>();
            Map<String, Set<String>> subcategoriesByCategory = new HashMap<>();
            for (CategoryResponseDTO category : categories) {
                String normalizedCategory = normalize(category.getName());
                categoriesByName.put(normalizedCategory, category);
                Set<String> normalizedSubcategories = new HashSet<>();
                if (category.getSubcategories() != null) {
                    for (CategoryResponseDTO.SubcategoryResponseDTO subcategory : category.getSubcategories()) {
                        normalizedSubcategories.add(normalize(subcategory.getName()));
                    }
                }
                subcategoriesByCategory.put(normalizedCategory, normalizedSubcategories);
            }
            return new CatalogIndex(categoriesByName, subcategoriesByCategory);
        }

        boolean containsCategory(String category) {
            return categoriesByName.containsKey(normalize(category));
        }

        boolean containsSubcategory(String category, String subcategory) {
            return subcategoriesByCategory.getOrDefault(normalize(category), Set.of()).contains(normalize(subcategory));
        }

        boolean isSensitive(String category) {
            CategoryResponseDTO dto = categoriesByName.get(normalize(category));
            return dto != null && dto.isSensitive();
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        }
    }
}
