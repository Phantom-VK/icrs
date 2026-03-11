package com.college.icrs.ai.service;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.model.Category;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Priority;
import com.college.icrs.model.Sentiment;
import com.college.icrs.model.Status;
import com.college.icrs.service.GrievanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class AgenticAiService {

    private final ChatModel chatModel;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final GrievanceService grievanceService;
    private final ObjectMapper objectMapper;
    private final IcrsProperties icrsProperties;

    @Value("${ai.modelname:deepseek-chat}")
    private String modelName;

    public Grievance processNewGrievance(Long grievanceId) {
        Grievance grievance = grievanceService.getGrievanceById(grievanceId);
        if (!icrsProperties.getAi().isEnabled()) {
            return grievance;
        }

        try {
            SentimentAnalysisService.SentimentDecision sentimentDecision =
                    sentimentAnalysisService.analyze(grievance.getDescription());

            ClassificationDecision classification = classify(grievance);
            Priority priority = parsePriority(classification.getPriority());
            Sentiment sentiment = sentimentDecision.sentiment();
            Double classificationConfidence = clampConfidence(classification.getConfidence());
            Double sentimentConfidence = clampConfidence(sentimentDecision.confidence());
            Double metadataConfidence = clampConfidence(combineConfidence(classificationConfidence, sentimentConfidence));
            String aiTitle = normalizeTitle(classification.getAiTitle(), grievance.getTitle());

            grievanceService.applyAiDecisionMetadata(
                    grievanceId,
                    priority,
                    sentiment,
                    aiTitle,
                    metadataConfidence,
                    mergeModelNames(modelName, sentimentDecision.modelName()),
                    decisionSource(),
                    LocalDateTime.now()
            );

            ResolutionDecision resolution = resolve(grievance, classification, sentiment);
            Double finalConfidence = clampConfidence(combineConfidence(classificationConfidence, resolution.getConfidence()));
            boolean autoResolve = shouldAutoResolve(grievance, resolution, finalConfidence);

            if (autoResolve) {
                String resolutionText = normalizeText(
                        resolution.getResolutionText(),
                        "Your grievance has been processed and marked resolved by the automated assistant."
                );
                String internalComment = normalizeText(
                        resolution.getInternalComment(),
                        "Auto-resolved after AI confidence and policy checks."
                );

                Grievance updated = grievanceService.markResolvedByAi(
                        grievanceId,
                        resolutionText,
                        internalComment,
                        finalConfidence,
                        mergeModelNames(modelName, sentimentDecision.modelName()),
                        decisionSource()
                );

                grievanceService.addSystemComment(
                        grievanceId,
                        systemUserEmail(),
                        "[AI Auto-Resolution]\n" + resolutionText
                );
                return updated;
            }

            String manualReviewComment = buildManualReviewComment(resolution, finalConfidence);
            Grievance updated = grievanceService.updateAiRecommendation(
                    grievanceId,
                    normalizeNullable(resolution.getResolutionText()),
                    manualReviewComment,
                    finalConfidence,
                    mergeModelNames(modelName, sentimentDecision.modelName()),
                    decisionSource(),
                    LocalDateTime.now()
            );

            if (icrsProperties.getAi().isAddSystemCommentOnManualReview()) {
                grievanceService.addSystemComment(
                        grievanceId,
                        systemUserEmail(),
                        "[AI Triage Suggestion]\n" + manualReviewComment
                );
            }
            return updated;

        } catch (Exception e) {
            // AI workflow must never break grievance creation flow.
            log.error("AI processing failed for grievanceId={}", grievanceId, e);
            return grievance;
        }
    }

    private ClassificationDecision classify(Grievance grievance) throws Exception {
        String prompt = """
                You are a grievance triage classifier.
                Return ONLY valid JSON. Do not include markdown fences.

                JSON schema:
                {
                  "priority": "LOW|MEDIUM|HIGH",
                  "aiTitle": "short summary title (max 120 chars)",
                  "summary": "brief summary for internal triage (max 240 chars)",
                  "confidence": 0.0
                }

                Context:
                - title: %s
                - description: %s
                - category: %s
                - subcategory: %s
                """.formatted(
                safe(grievance.getTitle()),
                safe(truncate(grievance.getDescription(), icrsProperties.getAi().getMaxDescriptionChars())),
                grievance.getCategory() != null ? safe(grievance.getCategory().getName()) : "UNKNOWN",
                grievance.getSubcategory() != null ? safe(grievance.getSubcategory().getName()) : "UNKNOWN"
        );

        String raw = chatModel.chat(prompt);
        return parseJson(raw, ClassificationDecision.class);
    }

    private ResolutionDecision resolve(
            Grievance grievance,
            ClassificationDecision classification,
            Sentiment sentiment
    ) throws Exception {
        String prompt = """
                You are an AI grievance resolver.
                Decide if this grievance can be auto-resolved safely.
                Return ONLY valid JSON. Do not include markdown fences.

                JSON schema:
                {
                  "autoResolve": true,
                  "resolutionText": "student-facing resolution text if autoResolve=true, else empty",
                  "internalComment": "explain why it was auto-resolved or routed to human",
                  "confidence": 0.0
                }

                Rules:
                - If unsure, set autoResolve to false.
                - Keep resolutionText concise and actionable.
                - Never claim actions you cannot verify.

                Grievance:
                - title: %s
                - description: %s
                - category: %s
                - subcategory: %s

                Classification:
                - sentiment: %s
                - priority: %s
                - summary: %s
                - confidence: %s
                """.formatted(
                safe(grievance.getTitle()),
                safe(truncate(grievance.getDescription(), icrsProperties.getAi().getMaxDescriptionChars())),
                grievance.getCategory() != null ? safe(grievance.getCategory().getName()) : "UNKNOWN",
                grievance.getSubcategory() != null ? safe(grievance.getSubcategory().getName()) : "UNKNOWN",
                sentiment != null ? sentiment.name() : "UNKNOWN",
                safe(classification.getPriority()),
                safe(classification.getSummary()),
                classification.getConfidence() == null ? "null" : classification.getConfidence()
        );

        String raw = chatModel.chat(prompt);
        return parseJson(raw, ResolutionDecision.class);
    }

    private boolean shouldAutoResolve(Grievance grievance, ResolutionDecision resolution, Double confidence) {
        if (resolution == null || !Boolean.TRUE.equals(resolution.getAutoResolve())) return false;
        if (grievance.getStatus() == Status.RESOLVED) return false;
        if (isSensitiveCategory(grievance)) return false;
        if (!StringUtils.hasText(resolution.getResolutionText())) return false;
        if (confidence == null) return false;
        return confidence >= icrsProperties.getAi().getAutoResolveConfidenceThreshold();
    }

    private boolean isSensitiveCategory(Grievance grievance) {
        try {
            Category category = grievance.getCategory();
            return category != null
                    && (Boolean.TRUE.equals(category.getSensitive()) || Boolean.TRUE.equals(category.getHideIdentity()));
        } catch (Exception e) {
            // Fail safe: if category information cannot be read, avoid auto resolution.
            log.warn("Could not determine sensitivity for grievanceId={}", grievance.getId(), e);
            return true;
        }
    }

    private String buildManualReviewComment(ResolutionDecision resolution, Double confidence) {
        String base = normalizeText(
                resolution != null ? resolution.getInternalComment() : null,
                "Routed for human review due to policy or confidence."
        );
        return base + " | confidence=" + (confidence == null ? "N/A" : confidence);
    }

    private String decisionSource() {
        return StringUtils.hasText(icrsProperties.getAi().getDecisionSource())
                ? icrsProperties.getAi().getDecisionSource()
                : "DEEPSEEK_AGENTIC_V1";
    }

    private String systemUserEmail() {
        return StringUtils.hasText(icrsProperties.getAi().getSystemUserEmail())
                ? icrsProperties.getAi().getSystemUserEmail()
                : "ai.system@icrs.local";
    }

    private String mergeModelNames(String llmModelName, String sentimentModelName) {
        if (!StringUtils.hasText(sentimentModelName)) {
            return llmModelName;
        }
        return llmModelName + " + sentiment:" + sentimentModelName;
    }

    private Priority parsePriority(String value) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return Priority.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Double combineConfidence(Double a, Double b) {
        if (a == null) return b;
        if (b == null) return a;
        return (a + b) / 2.0d;
    }

    private Double clampConfidence(Double confidence) {
        if (confidence == null) return null;
        if (confidence < 0d) return 0d;
        if (confidence > 1d) return 1d;
        return confidence;
    }

    private String normalizeTitle(String candidate, String fallback) {
        String title = normalizeText(candidate, fallback);
        return truncate(title, 120);
    }

    private String normalizeText(String value, String fallback) {
        if (!StringUtils.hasText(value)) return fallback;
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || maxLength <= 0) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private <T> T parseJson(String raw, Class<T> clazz) throws Exception {
        String json = extractJsonObject(raw);
        return objectMapper.readValue(json, clazz);
    }

    private String extractJsonObject(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new IllegalArgumentException("AI returned empty response");
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replace("```json", "").replace("```", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("AI response did not contain JSON object");
        }
        return text.substring(start, end + 1);
    }

    @Getter
    @Setter
    public static class ClassificationDecision {
        private String priority;
        private String aiTitle;
        private String summary;
        private Double confidence;
    }

    @Getter
    @Setter
    public static class ResolutionDecision {
        private Boolean autoResolve;
        private String resolutionText;
        private String internalComment;
        private Double confidence;
    }
}
