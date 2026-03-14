package com.college.icrs.rag;

import com.college.icrs.model.Grievance;
import com.college.icrs.model.Priority;
import com.college.icrs.model.Sentiment;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GrievanceVectorDocumentFactory {

    public static final String GRIEVANCE_ID_METADATA_KEY = "grievanceId";
    public static final String TITLE_METADATA_KEY = "title";
    public static final String DESCRIPTION_METADATA_KEY = "description";
    public static final String CATEGORY_METADATA_KEY = "category";
    public static final String SUBCATEGORY_METADATA_KEY = "subcategory";
    public static final String REGISTRATION_NUMBER_METADATA_KEY = "registrationNumber";
    public static final String PRIORITY_METADATA_KEY = "priority";
    public static final String SENTIMENT_METADATA_KEY = "sentiment";
    public static final String RESOLUTION_TEXT_METADATA_KEY = "resolutionText";
    public static final String SOURCE_METADATA_KEY = "source";

    public Document fromGrievance(Grievance grievance) {
        return fromParts(
                String.valueOf(grievance.getId()),
                grievance.getId(),
                grievance.getTitle(),
                grievance.getDescription(),
                grievance.getCategory() != null ? grievance.getCategory().getName() : null,
                grievance.getSubcategory() != null ? grievance.getSubcategory().getName() : null,
                grievance.getRegistrationNumber(),
                grievance.getPriority(),
                grievance.getSentiment(),
                grievance.getAiResolutionText(),
                "application-grievance"
        );
    }

    public Document fromImportedRecord(ImportedGrievanceRecord grievance) {
        return fromParts(
                grievance.documentId(),
                grievance.grievanceId(),
                grievance.title(),
                grievance.description(),
                grievance.category(),
                grievance.subcategory(),
                grievance.registrationNumber(),
                grievance.priority(),
                grievance.sentiment(),
                grievance.resolutionText(),
                "manual-import"
        );
    }

    public String buildContent(
            String title,
            String description,
            String category,
            String subcategory,
            String registrationNumber
    ) {
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "Title", title);
        appendSection(builder, "Description", description);
        appendSection(builder, "Category", category);
        appendSection(builder, "Subcategory", subcategory);
        appendSection(builder, "Registration Number", registrationNumber);
        return builder.toString().trim();
    }

    private Document fromParts(
            String documentId,
            Long grievanceId,
            String title,
            String description,
            String category,
            String subcategory,
            String registrationNumber,
            Priority priority,
            Sentiment sentiment,
            String resolutionText,
            String source
    ) {
        return Document.builder()
                .id(documentId)
                .text(buildContent(title, description, category, subcategory, registrationNumber))
                .metadata(metadataFor(
                        grievanceId,
                        title,
                        description,
                        category,
                        subcategory,
                        registrationNumber,
                        priority != null ? priority.name() : null,
                        sentiment != null ? sentiment.name() : null,
                        resolutionText,
                        source
                ))
                .build();
    }

    private Map<String, Object> metadataFor(
            Long grievanceId,
            String title,
            String description,
            String category,
            String subcategory,
            String registrationNumber,
            String priority,
            String sentiment,
            String resolutionText,
            String source
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (grievanceId != null) {
            metadata.put(GRIEVANCE_ID_METADATA_KEY, grievanceId);
        }
        putIfHasText(metadata, TITLE_METADATA_KEY, title);
        putIfHasText(metadata, DESCRIPTION_METADATA_KEY, description);
        putIfHasText(metadata, CATEGORY_METADATA_KEY, category);
        putIfHasText(metadata, SUBCATEGORY_METADATA_KEY, subcategory);
        putIfHasText(metadata, REGISTRATION_NUMBER_METADATA_KEY, registrationNumber);
        putIfHasText(metadata, PRIORITY_METADATA_KEY, priority);
        putIfHasText(metadata, SENTIMENT_METADATA_KEY, sentiment);
        putIfHasText(metadata, RESOLUTION_TEXT_METADATA_KEY, resolutionText);
        putIfHasText(metadata, SOURCE_METADATA_KEY, source);
        return metadata;
    }

    private void putIfHasText(Map<String, Object> metadata, String key, String value) {
        if (StringUtils.hasText(value)) {
            metadata.put(key, value.trim());
        }
    }

    private void appendSection(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(label).append(": ").append(value.trim());
    }

    public record ImportedGrievanceRecord(
            String documentId,
            Long grievanceId,
            String title,
            String description,
            String category,
            String subcategory,
            String registrationNumber,
            Priority priority,
            Sentiment sentiment,
            String resolutionText
    ) {
    }
}
