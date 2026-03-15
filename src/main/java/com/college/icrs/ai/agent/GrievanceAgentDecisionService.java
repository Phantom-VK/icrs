package com.college.icrs.ai.agent;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Sentiment;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceAgentDecisionService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final IcrsProperties icrsProperties;

    public ContextToolSelection selectContextTools(
            Grievance grievance,
            Sentiment sentiment,
            String ragContext,
            String policyContext,
            String commentContext,
            String statusHistoryContext,
            boolean policyFetched,
            boolean commentFetched,
            boolean statusHistoryFetched,
            int plannerIteration
    ) {
        log.info(IcrsLog.event("ai.context-planner.requested", "grievanceId", grievance.getId()));
        String prompt = """
                You are deciding the single next step for an AI grievance agent before classification and resolution.
                Return ONLY valid JSON.

                JSON schema:
                {
                  "nextTool": "POLICY|COMMENT|STATUS_HISTORY|CLASSIFY",
                  "reason": "short explanation"
                }

                Rules:
                - Choose exactly one next step.
                - Choose POLICY when institutional rules, sensitivity, assignment, or privacy constraints may matter and policy has not already been fetched.
                - Choose COMMENT only if the conversation thread could materially change triage or resolution and comments have not already been fetched.
                - Choose STATUS_HISTORY only if prior transitions may matter and status history has not already been fetched.
                - Choose CLASSIFY when the current context is sufficient.
                - Never request a tool that has already been fetched.

                Context:
                - title: %s
                - description: %s
                - category: %s
                - subcategory: %s
                - current sentiment: %s
                - plannerIteration: %d
                - policyFetched: %s
                - commentFetched: %s
                - statusHistoryFetched: %s
                %s
                %s
                %s
                %s
                """.formatted(
                safe(grievance.getTitle()),
                safe(truncate(grievance.getDescription(), icrsProperties.getAi().getMaxDescriptionChars())),
                grievance.getCategory() != null ? safe(grievance.getCategory().getName()) : "UNKNOWN",
                grievance.getSubcategory() != null ? safe(grievance.getSubcategory().getName()) : "UNKNOWN",
                sentiment != null ? sentiment.name() : "UNKNOWN",
                plannerIteration,
                policyFetched,
                commentFetched,
                statusHistoryFetched,
                formatPromptSection("Retrieved cases", ragContext),
                formatPromptSection("Current policy context", policyContext),
                formatPromptSection("Current comment context", commentContext),
                formatPromptSection("Current status history", statusHistoryContext)
        );

        try {
            String raw = chatModel.chat(prompt);
            ContextToolSelection selection = parseJson(raw, ContextToolSelection.class);
            return selection != null ? selection.normalized() : ContextToolSelection.classify();
        } catch (Exception e) {
            log.warn(IcrsLog.event("ai.context-planner.failed",
                    "grievanceId", grievance.getId(),
                    "reason", e.getClass().getSimpleName()), e);
            return ContextToolSelection.classify();
        }
    }

    public ClassificationDecision classify(
            Grievance grievance,
            Sentiment sentiment,
            String ragContext,
            String policyContext,
            String commentContext,
            String statusHistoryContext
    ) throws Exception {
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
                %s
                %s
                %s
                """.formatted(
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

        String raw = chatModel.chat(prompt);
        log.debug(IcrsLog.event("ai.classification.raw-response.received", "grievanceId", grievance.getId()));
        return parseJson(raw, ClassificationDecision.class);
    }

    public ResolutionDecision resolve(
            Grievance grievance,
            Sentiment sentiment,
            String ragContext,
            String policyContext,
            String commentContext,
            String statusHistoryContext
    ) throws Exception {
        log.info(IcrsLog.event("ai.resolution.requested", "grievanceId", grievance.getId()));
        String classificationTitle = normalizeTitle(grievance.getAiTitle(), grievance.getTitle());
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
                %s
                %s
                %s
                """.formatted(
                safe(grievance.getTitle()),
                safe(truncate(grievance.getDescription(), icrsProperties.getAi().getMaxDescriptionChars())),
                grievance.getCategory() != null ? safe(grievance.getCategory().getName()) : "UNKNOWN",
                grievance.getSubcategory() != null ? safe(grievance.getSubcategory().getName()) : "UNKNOWN",
                sentiment != null ? sentiment.name() : "UNKNOWN",
                classificationTitle,
                formatPromptSection("Retrieved cases", ragContext),
                formatPromptSection("Policy signals", policyContext),
                formatPromptSection("Comment thread", commentContext),
                formatPromptSection("Status history", statusHistoryContext)
        );

        String raw = chatModel.chat(prompt);
        log.debug(IcrsLog.event("ai.resolution.raw-response.received", "grievanceId", grievance.getId()));
        return parseJson(raw, ResolutionDecision.class);
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
}
