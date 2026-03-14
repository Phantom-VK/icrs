package com.college.icrs.ai.service;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Category;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Priority;
import com.college.icrs.model.Sentiment;
import com.college.icrs.model.Status;
import com.college.icrs.rag.RagService;
import com.college.icrs.rag.RagService.GrievanceContext;
import com.college.icrs.service.GrievanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
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
    private final RagService ragService;

    @Value("${ai.modelname:deepseek-chat}")
    private String modelName;

    @Async("aiTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNewGrievanceAsync(Long grievanceId) {
        log.info(IcrsLog.event("ai.workflow.async-dispatched", "grievanceId", grievanceId));
        processNewGrievance(grievanceId);
    }

    public Grievance processNewGrievance(Long grievanceId) {
        log.info(IcrsLog.event("ai.workflow.start", "grievanceId", grievanceId));
        Grievance grievance = grievanceService.getGrievanceById(grievanceId);
        if (!icrsProperties.getAi().isEnabled()) {
            log.info(IcrsLog.event("ai.workflow.skipped", "grievanceId", grievanceId, "reason", "ai-disabled"));
            return grievance;
        }

        try {
            SentimentAnalysisService.SentimentDecision sentimentDecision =
                    sentimentAnalysisService.analyze(grievance.getDescription());
            Sentiment sentiment = sentimentDecision.sentiment();

            List<GrievanceContext> contexts = ragService.retrieveSimilar(grievance);
            String contextSection = ragService.buildContextSection(contexts);

            ClassificationDecision classification = classify(grievance, sentiment, contextSection);
            Priority priority = parsePriority(classification.getPriority());
            Double classificationConfidence = clampConfidence(classification.getConfidence());
            String classificationTitle = normalizeTitle(classification.getAiTitle(), grievance.getTitle());

            grievanceService.applyAiDecisionMetadata(
                    grievanceId,
                    priority,
                    sentiment,
                    classificationTitle,
                    classificationConfidence,
                    mergeModelNames(modelName, sentimentDecision.modelName()),
                    decisionSource(),
                    LocalDateTime.now()
            );

            ResolutionDecision resolution = resolve(grievance, sentiment, contextSection, classificationTitle);
            Double resolutionConfidence = clampConfidence(resolution.getConfidence());

            Double metadataConfidence = clampConfidence(combineConfidence(classificationConfidence, resolutionConfidence));
            boolean autoResolve = shouldAutoResolve(grievance, resolution, metadataConfidence);
            log.info(IcrsLog.event("ai.workflow.decision.completed",
                    "grievanceId", grievanceId,
                    "priority", priority,
                    "autoResolve", autoResolve,
                    "confidence", metadataConfidence));

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
                        metadataConfidence,
                        mergeModelNames(modelName, sentimentDecision.modelName()),
                        decisionSource()
                );

                grievanceService.addSystemComment(
                        grievanceId,
                        systemUserEmail(),
                        "[AI Auto-Resolution]\n" + resolutionText
                );
                log.info(IcrsLog.event("ai.workflow.completed", "grievanceId", grievanceId, "outcome", "auto-resolved"));
                return updated;
            }

            String manualReviewComment = buildManualReviewComment(resolution);
            Grievance updated = grievanceService.updateAiRecommendation(
                    grievanceId,
                    normalizeNullable(resolution.getResolutionText()),
                    manualReviewComment,
                    metadataConfidence,
                    mergeModelNames(modelName, sentimentDecision.modelName()),
                    decisionSource(),
                    LocalDateTime.now()
            );
            log.info(IcrsLog.event("ai.workflow.completed", "grievanceId", grievanceId, "outcome", "manual-review"));
            return updated;

        } catch (Exception e) {
            log.error(IcrsLog.event("ai.workflow.failed", "grievanceId", grievanceId, "reason", e.getClass().getSimpleName()), e);
            return grievance;
        }
    }

    private ClassificationDecision classify(Grievance grievance, Sentiment sentiment, String context) throws Exception {
        log.info(IcrsLog.event("ai.classification.requested", "grievanceId", grievance.getId()));
        String prompt = """
                You are an AI grievance triage classifier for a college.
                Return ONLY valid JSON.

                JSON schema:
                {
                  "priority": "LOW|MEDIUM|HIGH",
                  "aiTitle": "short summary title (max 120 chars)",
                  "confidence": 0.0
                }

                Rules:
                - Base your priority on the grievance details, category, and sentiment.
                - Provide confidence between 0 and 1.
                - Keep titles concise and factual.

                Context:
                - title: %s
                - description: %s
                - category: %s
                - subcategory: %s
                - sentiment: %s
                %s
                """.formatted(
                safe(grievance.getTitle()),
                safe(truncate(grievance.getDescription(), icrsProperties.getAi().getMaxDescriptionChars())),
                grievance.getCategory() != null ? safe(grievance.getCategory().getName()) : "UNKNOWN",
                grievance.getSubcategory() != null ? safe(grievance.getSubcategory().getName()) : "UNKNOWN",
                sentiment != null ? sentiment.name() : "UNKNOWN",
                StringUtils.hasText(context) ? "Retrieved cases:\n" + context : ""
        );

        String raw = chatModel.chat(prompt);
        log.debug(IcrsLog.event("ai.classification.raw-response.received", "grievanceId", grievance.getId()));
        return parseJson(raw, ClassificationDecision.class);
    }

    private ResolutionDecision resolve(Grievance grievance, Sentiment sentiment, String context, String classificationTitle) throws Exception {
        log.info(IcrsLog.event("ai.resolution.requested", "grievanceId", grievance.getId()));
        String prompt = """
                You are an AI resolution assistant for a college grievance system.
                Return ONLY valid JSON.

                JSON schema:
                {
                  "autoResolve": true,
                  "resolutionText": "student facing resolution text",
                  "internalComment": "internal reasoning for operators",
                  "confidence": 0.0
                }

                Rules:
                - Provide autoResolve=false if you are unsure or the category is sensitive.
                - Keep resolutionText concise, factual, and actionable.
                - Never claim to take actions you cannot verify.

                Context:
                - title: %s
                - description: %s
                - category: %s
                - subcategory: %s
                - sentiment: %s
                - classificationTitle: %s
                %s
                """.formatted(
                safe(grievance.getTitle()),
                safe(truncate(grievance.getDescription(), icrsProperties.getAi().getMaxDescriptionChars())),
                grievance.getCategory() != null ? safe(grievance.getCategory().getName()) : "UNKNOWN",
                grievance.getSubcategory() != null ? safe(grievance.getSubcategory().getName()) : "UNKNOWN",
                sentiment != null ? sentiment.name() : "UNKNOWN",
                classificationTitle,
                StringUtils.hasText(context) ? "Retrieved cases:\n" + context : ""
        );

        String raw = chatModel.chat(prompt);
        log.debug(IcrsLog.event("ai.resolution.raw-response.received", "grievanceId", grievance.getId()));
        return parseJson(raw, ResolutionDecision.class);
    }

    private boolean shouldAutoResolve(Grievance grievance, ResolutionDecision decision, Double confidence) {
        if (decision == null || !Boolean.TRUE.equals(decision.getAutoResolve())) return false;
        if (grievance.getStatus() == Status.RESOLVED) return false;
        if (isSensitiveCategory(grievance)) return false;
        if (!StringUtils.hasText(decision.getResolutionText())) return false;
        if (confidence == null) return false;
        return confidence >= icrsProperties.getAi().getAutoResolveConfidenceThreshold();
    }

    private boolean isSensitiveCategory(Grievance grievance) {
        try {
            Category category = grievance.getCategory();
            return category != null
                    && (Boolean.TRUE.equals(category.getSensitive()) || Boolean.TRUE.equals(category.getHideIdentity()));
        } catch (Exception e) {
            log.warn("Could not determine sensitivity for grievanceId={}", grievance.getId(), e);
            return true;
        }
    }

    private String buildManualReviewComment(ResolutionDecision decision) {
        return normalizeText(
                decision != null ? decision.getInternalComment() : null,
                "Routed for human review due to policy or confidence."
        );
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
    private static class ClassificationDecision {
        private String priority;
        private String aiTitle;
        private Double confidence;
    }

    @Getter
    @Setter
    private static class ResolutionDecision {
        private Boolean autoResolve;
        private String resolutionText;
        private String internalComment;
        private Double confidence;
    }
}
