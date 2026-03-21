package com.college.icrs.ai.agent;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Sentiment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceAgentDecisionService {

    private static final int DECISION_TIMEOUT_BUFFER_SECONDS = 10;
    private static final double FALLBACK_CONFIDENCE = 0.35d;

    private final IcrsProperties icrsProperties;
    private final GrievanceClassifierAiService classifierAiService;
    private final GrievanceResolverAiService resolverAiService;

    public ClassificationDecision classify(
            Grievance grievance,
            Sentiment sentiment,
            String ragContext,
            String policyContext,
            String commentContext,
            String statusHistoryContext
    ) throws Exception {
        log.info(IcrsLog.event("ai.classification.requested", "grievanceId", grievance.getId()));
        try {
            return CompletableFuture.supplyAsync(() -> classifierAiService.classify(
                    safe(grievance.getTitle()),
                    safe(truncate(grievance.getDescription(), icrsProperties.getAi().getMaxDescriptionChars())),
                    grievance.getCategory() != null ? safe(grievance.getCategory().getName()) : "UNKNOWN",
                    grievance.getSubcategory() != null ? safe(grievance.getSubcategory().getName()) : "UNKNOWN",
                    sentiment != null ? sentiment.name() : "UNKNOWN",
                    formatPromptSection("Retrieved cases", ragContext),
                    formatPromptSection("Policy signals", policyContext),
                    formatPromptSection("Comment thread", commentContext),
                    formatPromptSection("Status history", statusHistoryContext)
            )).orTimeout(decisionTimeoutSeconds(), TimeUnit.SECONDS).join();
        } catch (Exception e) {
            log.warn(IcrsLog.event("ai.classification.fallback",
                    "grievanceId", grievance.getId(),
                    "reason", e.getClass().getSimpleName()));
            return fallbackClassification(grievance);
        }
    }

    public ResolutionDecision resolve(
            Grievance grievance,
            Sentiment sentiment,
            String ragContext,
            String policyContext,
            String commentContext,
            String statusHistoryContext,
            String resolutionGuidanceContext
    ) throws Exception {
        log.info(IcrsLog.event("ai.resolution.requested", "grievanceId", grievance.getId()));
        String classificationTitle = normalizeTitle(grievance.getAiTitle(), grievance.getTitle());
        try {
            return CompletableFuture.supplyAsync(() -> resolverAiService.resolve(
                    safe(grievance.getTitle()),
                    safe(truncate(grievance.getDescription(), icrsProperties.getAi().getMaxDescriptionChars())),
                    grievance.getCategory() != null ? safe(grievance.getCategory().getName()) : "UNKNOWN",
                    grievance.getSubcategory() != null ? safe(grievance.getSubcategory().getName()) : "UNKNOWN",
                    sentiment != null ? sentiment.name() : "UNKNOWN",
                    classificationTitle,
                    formatPromptSection("Retrieved cases", ragContext),
                    formatPromptSection("Policy signals", policyContext),
                    formatPromptSection("Comment thread", commentContext),
                    formatPromptSection("Status history", statusHistoryContext),
                    formatPromptSection("Resolution guidance", resolutionGuidanceContext)
            )).orTimeout(decisionTimeoutSeconds(), TimeUnit.SECONDS).join();
        } catch (Exception e) {
            log.warn(IcrsLog.event("ai.resolution.fallback",
                    "grievanceId", grievance.getId(),
                    "reason", e.getClass().getSimpleName()));
            return fallbackResolution();
        }
    }

    private String normalizeTitle(String candidate, String fallback) {
        String title = normalizeText(candidate, fallback);
        return truncate(title, 120);
    }

    private String normalizeText(String value, String fallback) {
        if (!StringUtils.hasText(value)) return fallback;
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

    private String formatPromptSection(String title, String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return title + ":\n" + content;
    }

    private long decisionTimeoutSeconds() {
        return Math.max(15, icrsProperties.getAi().getTimeoutSeconds() + DECISION_TIMEOUT_BUFFER_SECONDS);
    }

    private ClassificationDecision fallbackClassification(Grievance grievance) {
        ClassificationDecision decision = new ClassificationDecision();
        decision.setPriority("MEDIUM");
        decision.setAiTitle(normalizeTitle(grievance.getTitle(), grievance.getTitle()));
        decision.setConfidence(FALLBACK_CONFIDENCE);
        return decision;
    }

    private ResolutionDecision fallbackResolution() {
        ResolutionDecision decision = new ResolutionDecision();
        decision.setAutoResolve(Boolean.FALSE);
        decision.setResolutionText("Your grievance has been forwarded to the assigned college office for manual review and further action.");
        decision.setInternalComment("Routed for human review after AI decision timeout.");
        decision.setConfidence(FALLBACK_CONFIDENCE);
        return decision;
    }
}
