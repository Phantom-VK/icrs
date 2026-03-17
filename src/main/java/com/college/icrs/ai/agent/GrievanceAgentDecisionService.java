package com.college.icrs.ai.agent;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Sentiment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceAgentDecisionService {

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
        return classifierAiService.classify(
                safe(grievance.getTitle()),
                safe(truncate(grievance.getDescription(), icrsProperties.getAi().getMaxDescriptionChars())),
                grievance.getCategory() != null ? safe(grievance.getCategory().getName()) : "UNKNOWN",
                grievance.getSubcategory() != null ? safe(grievance.getSubcategory().getName()) : "UNKNOWN",
                sentiment != null ? sentiment.name() : "UNKNOWN",
                formatPromptSection("Retrieved cases", ragContext),
                formatPromptSection("Policy signals", policyContext),
                formatPromptSection("Comment thread", commentContext),
                formatPromptSection("Status history", statusHistoryContext)
        );
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
        return resolverAiService.resolve(
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
        );
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
}
