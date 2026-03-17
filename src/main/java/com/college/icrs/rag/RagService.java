package com.college.icrs.rag;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Priority;
import com.college.icrs.model.Sentiment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import com.college.icrs.repository.GrievanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final VectorStore vectorStore;
    private final IcrsProperties properties;
    private final EmbeddingService embeddingService;
    private final GrievanceRepository grievanceRepository;

    public List<GrievanceContext> retrieveSimilar(Grievance grievance) {
        if (grievance == null || !properties.getAi().getRag().isEnabled()) {
            return List.of();
        }

        String query = embeddingService.buildEmbeddingText(grievance);
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        int limit = Math.max(1, properties.getAi().getRag().getTopK());
        try {
            List<GrievanceContext> contexts = vectorStore.similaritySearch(
                            SearchRequest.builder()
                                    .query(query)
                                    .topK(limit + 1)
                                    .build()
                    ).stream()
                    .filter(document -> !String.valueOf(grievance.getId()).equals(document.getId()))
                    .map(this::map)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(GrievanceContext::getSimilarityScore, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limit)
                    .toList();
            log.info(IcrsLog.event("rag.retrieve", "grievanceId", grievance.getId(), "count", contexts.size()));
            return contexts;
        } catch (Exception e) {
            log.error(IcrsLog.event("rag.retrieve.failed", "grievanceId", grievance.getId()), e);
            return List.of();
        }
    }

    public String buildContextSection(List<GrievanceContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "";
        }
        return contexts.stream()
                .map(ctx -> String.format(
                        "- Reference %s (similarity=%s, priority=%s, sentiment=%s): title=%s; description=%s; priorResolution=%s",
                        ctx.getReferenceId(),
                        formatScore(ctx.getSimilarityScore()),
                        ctx.getPriority(),
                        ctx.getSentiment(),
                        truncate(ctx.getTitle(), 120),
                        truncate(ctx.getDescription(), 240),
                        truncate(ctx.getResolutionText(), 180)))
                .collect(Collectors.joining("\n"));
    }

    @Getter
    @Setter
    public static class GrievanceContext {
        private String referenceId;
        private String title;
        private String description;
        private Priority priority;
        private Sentiment sentiment;
        private String resolutionText;
        private Double similarityScore;
    }

    private GrievanceContext map(Document document) {
        Long grievanceId = grievanceId(document);
        if (grievanceId != null) {
            return grievanceRepository.findById(grievanceId)
                    .map(grievance -> {
                        GrievanceContext context = new GrievanceContext();
                        context.setReferenceId(String.valueOf(grievanceId));
                        context.setTitle(grievance.getTitle());
                        context.setDescription(grievance.getDescription());
                        context.setPriority(grievance.getPriority());
                        context.setSentiment(grievance.getSentiment());
                        context.setResolutionText(grievance.getAiResolutionText());
                        context.setSimilarityScore(document.getScore());
                        return context;
                    })
                    .orElseGet(() -> fromDocument(document));
        }
        return fromDocument(document);
    }

    private Long grievanceId(Document document) {
        String metadataValue = metadataText(document, GrievanceVectorDocumentFactory.GRIEVANCE_ID_METADATA_KEY);
        if (StringUtils.hasText(metadataValue)) {
            try {
                return Long.parseLong(metadataValue);
            } catch (NumberFormatException ex) {
                log.warn(IcrsLog.event("rag.retrieve.invalid-grievance-metadata-id",
                        "documentId", document.getId(),
                        "grievanceId", metadataValue), ex);
                return null;
            }
        }

        if (isNumeric(document.getId())) {
            try {
                return Long.parseLong(document.getId());
            } catch (NumberFormatException ex) {
                log.warn(IcrsLog.event("rag.retrieve.invalid-document-id", "documentId", document.getId()), ex);
                return null;
            }
        }

        if (!isManualImport(document)) {
            log.debug(IcrsLog.event("rag.retrieve.non-numeric-document-id", "documentId", document.getId()));
        }
        return null;
    }

    private boolean isManualImport(Document document) {
        String source = metadataText(document, GrievanceVectorDocumentFactory.SOURCE_METADATA_KEY);
        return "manual-import".equalsIgnoreCase(source);
    }

    private boolean isNumeric(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private GrievanceContext fromDocument(Document document) {
        String title = metadataText(document, GrievanceVectorDocumentFactory.TITLE_METADATA_KEY);
        String description = metadataText(document, GrievanceVectorDocumentFactory.DESCRIPTION_METADATA_KEY);
        if (!StringUtils.hasText(title) && !StringUtils.hasText(description)) {
            return null;
        }

        GrievanceContext context = new GrievanceContext();
        context.setReferenceId(document.getId());
        context.setTitle(title);
        context.setDescription(description);
        context.setPriority(parsePriority(metadataText(document, GrievanceVectorDocumentFactory.PRIORITY_METADATA_KEY)));
        context.setSentiment(parseSentiment(metadataText(document, GrievanceVectorDocumentFactory.SENTIMENT_METADATA_KEY)));
        context.setResolutionText(metadataText(document, GrievanceVectorDocumentFactory.RESOLUTION_TEXT_METADATA_KEY));
        context.setSimilarityScore(document.getScore());
        return context;
    }

    private String metadataText(Document document, String key) {
        Object value = document.getMetadata().get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Priority parsePriority(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Sentiment parseSentiment(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Sentiment.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String formatScore(Double score) {
        if (score == null) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.3f", score);
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "n/a";
        }
        String trimmed = value.replace('\n', ' ').trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
