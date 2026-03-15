package com.college.icrs.ai.agent;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Category;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Priority;
import com.college.icrs.model.Sentiment;
import com.college.icrs.model.Status;
import com.college.icrs.service.GrievanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceAgentActionService {

    private final GrievanceService grievanceService;
    private final IcrsProperties icrsProperties;

    @Value("${ai.modelname:deepseek-chat}")
    private String modelName;

    public Grievance applyClassificationMetadata(
            Long grievanceId,
            Sentiment sentiment,
            String sentimentModelName,
            String priorityValue,
            String aiTitleValue,
            Double confidenceValue
    ) {
        Grievance grievance = grievanceService.getGrievanceById(grievanceId);
        Priority priority = parsePriority(priorityValue);
        Double classificationConfidence = clampConfidence(confidenceValue);
        String classificationTitle = normalizeTitle(aiTitleValue, grievance.getTitle());

        return grievanceService.applyAiDecisionMetadata(
                grievanceId,
                priority,
                sentiment,
                classificationTitle,
                classificationConfidence,
                mergeModelNames(modelName, sentimentModelName),
                decisionSource(),
                LocalDateTime.now()
        );
    }

    public Grievance finalizeDecision(
            Long grievanceId,
            String sentimentModelName,
            Double classificationConfidenceValue,
            Boolean resolutionAutoResolve,
            String resolutionTextValue,
            String resolutionInternalCommentValue,
            Double resolutionConfidenceValue
    ) {
        Grievance grievance = grievanceService.getGrievanceById(grievanceId);
        Double classificationConfidence = clampConfidence(classificationConfidenceValue);
        Double resolutionConfidence = clampConfidence(resolutionConfidenceValue);
        Double metadataConfidence = clampConfidence(combineConfidence(classificationConfidence, resolutionConfidence));
        boolean autoResolve = shouldAutoResolve(grievance, resolutionAutoResolve, resolutionTextValue, metadataConfidence);

        log.info(IcrsLog.event("ai.workflow.decision.completed",
                "grievanceId", grievance.getId(),
                "priority", grievance.getPriority(),
                "autoResolve", autoResolve,
                "confidence", metadataConfidence));

        if (autoResolve) {
            return finalizeAutoResolution(grievanceId, grievance, sentimentModelName, resolutionTextValue, resolutionInternalCommentValue, metadataConfidence);
        }

        String manualReviewComment = buildManualReviewComment(resolutionInternalCommentValue);
        Grievance updated = grievanceService.updateAiRecommendation(
                grievanceId,
                normalizeNullable(resolutionTextValue),
                manualReviewComment,
                metadataConfidence,
                mergeModelNames(modelName, sentimentModelName),
                decisionSource(),
                LocalDateTime.now()
        );
        log.info(IcrsLog.event("ai.workflow.completed", "grievanceId", grievance.getId(), "outcome", "manual-review"));
        return updated;
    }

    private Grievance finalizeAutoResolution(
            Long grievanceId,
            Grievance grievance,
            String sentimentModelName,
            String resolutionTextValue,
            String resolutionInternalCommentValue,
            Double metadataConfidence
    ) {
        String resolutionText = normalizeText(
                resolutionTextValue,
                "Your grievance has been processed and marked resolved by the automated assistant."
        );
        String internalComment = normalizeText(
                resolutionInternalCommentValue,
                "Auto-resolved after AI confidence and policy checks."
        );

        Grievance updated = grievanceService.markResolvedByAi(
                grievanceId,
                resolutionText,
                internalComment,
                metadataConfidence,
                mergeModelNames(modelName, sentimentModelName),
                decisionSource()
        );

        grievanceService.addSystemComment(
                grievanceId,
                systemUserEmail(),
                "[AI Auto-Resolution]\n" + resolutionText
        );
        log.info(IcrsLog.event("ai.workflow.completed", "grievanceId", grievance.getId(), "outcome", "auto-resolved"));
        return updated;
    }

    private boolean shouldAutoResolve(Grievance grievance, Boolean autoResolveRequested, String resolutionText, Double confidence) {
        if (!Boolean.TRUE.equals(autoResolveRequested)) return false;
        if (grievance.getStatus() == Status.RESOLVED) return false;
        if (isSensitiveCategory(grievance)) return false;
        if (!StringUtils.hasText(resolutionText)) return false;
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

    private String buildManualReviewComment(String internalComment) {
        return normalizeText(
                internalComment,
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
        return title.length() <= 120 ? title : title.substring(0, 120);
    }

    private String normalizeText(String value, String fallback) {
        if (!StringUtils.hasText(value)) return fallback;
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim();
    }
}
