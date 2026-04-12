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
    public static final String COMMENT_SUMMARY_METADATA_KEY = "commentSummary";
    public static final String SOURCE_METADATA_KEY = "source";

    public Document fromGrievance(Grievance grievance) {
        return fromGrievance(grievance, null);
    }

    public Document fromGrievance(Grievance grievance, String commentSummary) {
        return fromParts(
                String.valueOf(grievance.getId()),
                grievance.getId(),
                grievance.getTitle(),
                grievance.getDescription(),
                grievance.getCategory() != null ? grievance.getCategory().getName() : null,
                grievance.getSubcategory() != null ? grievance.getSubcategory().getName() : null,
                grievance.getRegistrationNumber(),
                resolvedCaseSummary(grievance),
                compactCommentSummary(commentSummary),
                grievance.getPriority(),
                grievance.getSentiment(),
                grievance.getAiResolutionText(),
                commentSummary,
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
                importedResolutionSummary(grievance.resolutionText()),
                importedCommentSummary(grievance.commentSummary()),
                grievance.priority(),
                grievance.sentiment(),
                grievance.resolutionText(),
                grievance.commentSummary(),
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
        return buildContent(title, description, category, subcategory, registrationNumber, null, null);
    }

    private String buildContent(
            String title,
            String description,
            String category,
            String subcategory,
            String registrationNumber,
            String resolutionSummary,
            String commentSummary
    ) {
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "Title", title);
        appendSection(builder, "Description", description);
        appendSection(builder, "Category", category);
        appendSection(builder, "Subcategory", subcategory);
        appendSection(builder, "Registration Number", registrationNumber);
        appendSection(builder, "Resolution Summary", resolutionSummary);
        appendSection(builder, "Comment Notes", commentSummary);
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
            String resolutionSummary,
            String commentSummary,
            Priority priority,
            Sentiment sentiment,
            String resolutionText,
            String commentSummaryMetadata,
            String source
    ) {
        return Document.builder()
                .id(documentId)
                .text(buildContent(title, description, category, subcategory, registrationNumber, resolutionSummary, commentSummary))
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
                        commentSummaryMetadata,
                        source
                ))
                .build();
    }

    private String resolvedCaseSummary(Grievance grievance) {
        if (grievance == null || !grievance.isAiResolved()) {
            return null;
        }
        return compactResolutionSummary(grievance.getAiResolutionText());
    }

    private String importedResolutionSummary(String resolutionText) {
        return compactResolutionSummary(resolutionText);
    }

    private String importedCommentSummary(String commentSummary) {
        return compactCommentSummary(commentSummary);
    }

    private String compactResolutionSummary(String resolutionText) {
        if (!StringUtils.hasText(resolutionText)) {
            return null;
        }
        String normalized = resolutionText.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 220 ? normalized : normalized.substring(0, 220);
    }

    private String compactCommentSummary(String commentSummary) {
        if (!StringUtils.hasText(commentSummary)) {
            return null;
        }
        String normalized = commentSummary.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 320 ? normalized : normalized.substring(0, 320);
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
            String commentSummary,
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
        putIfHasText(metadata, COMMENT_SUMMARY_METADATA_KEY, commentSummary);
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
            String resolutionText,
            String commentSummary
    ) {
    }
}
