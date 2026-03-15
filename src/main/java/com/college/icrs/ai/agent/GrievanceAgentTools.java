package com.college.icrs.ai.agent;

import com.college.icrs.ai.service.SentimentAnalysisService;
import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Category;
import com.college.icrs.model.Comment;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Priority;
import com.college.icrs.model.Sentiment;
import com.college.icrs.model.Status;
import com.college.icrs.model.StatusHistory;
import com.college.icrs.rag.RagService;
import com.college.icrs.repository.CommentRepository;
import com.college.icrs.repository.StatusHistoryRepository;
import com.college.icrs.service.GrievanceService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceAgentTools {

    private final ChatModel chatModel;
    private final GrievanceService grievanceService;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final RagService ragService;
    private final CommentRepository commentRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final ObjectMapper objectMapper;
    private final IcrsProperties icrsProperties;

    @Value("${ai.modelname:deepseek-chat}")
    private String modelName;

    public Grievance loadGrievance(Long grievanceId) {
        return grievanceService.getGrievanceById(grievanceId);
    }

    public SentimentAnalysisService.SentimentDecision analyzeSentiment(Grievance grievance) {
        return sentimentAnalysisService.analyze(grievance != null ? grievance.getDescription() : null);
    }

    public List<RagService.GrievanceContext> retrieveSimilar(Grievance grievance) {
        return ragService.retrieveSimilar(grievance);
    }

    public String buildContextSection(List<RagService.GrievanceContext> contexts) {
        return ragService.buildContextSection(contexts);
    }

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

    public String buildPolicyContext(Long grievanceId) {
        Grievance grievance = grievanceService.getGrievanceById(grievanceId);
        Category category = grievance.getCategory();
        return """
                - currentStatus: %s
                - sensitiveCategory: %s
                - hideIdentity: %s
                - assignedTo: %s
                """.formatted(
                grievance.getStatus(),
                category != null && Boolean.TRUE.equals(category.getSensitive()),
                category != null && Boolean.TRUE.equals(category.getHideIdentity()),
                grievance.getAssignedTo() != null ? safe(grievance.getAssignedTo().getEmail()) : "UNASSIGNED"
        ).trim();
    }

    public String buildCommentContext(Long grievanceId) {
        List<Comment> comments = commentRepository.findByGrievanceIdOrderByCreatedAtAsc(grievanceId);
        if (comments.isEmpty()) {
            return "No comments yet.";
        }
        return comments.stream()
                .skip(Math.max(0, comments.size() - 5L))
                .map(comment -> "- %s: %s".formatted(
                        comment.getAuthor() != null ? safe(comment.getAuthor().getUsername()) : "Unknown",
                        truncate(safe(comment.getBody()).replaceAll("\\s+", " "), 240)
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No comments yet.");
    }

    public String buildStatusHistoryContext(Long grievanceId) {
        List<StatusHistory> history = statusHistoryRepository.findByGrievanceIdOrderByChangedAtDesc(grievanceId);
        if (history.isEmpty()) {
            return "No prior status transitions.";
        }
        return history.stream()
                .limit(5)
                .map(entry -> "- %s -> %s (%s)".formatted(
                        entry.getFromStatus(),
                        entry.getToStatus(),
                        StringUtils.hasText(entry.getReason()) ? truncate(entry.getReason().replaceAll("\\s+", " "), 180) : "no reason"
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No prior status transitions.");
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
        String classificationTitle = normalizeTitle(
                aiTitleValue,
                grievance.getTitle()
        );

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

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClassificationDecision implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String priority;
        private String aiTitle;
        private Double confidence;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResolutionDecision implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Boolean autoResolve;
        private String resolutionText;
        private String internalComment;
        private Double confidence;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContextToolSelection implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private NextTool nextTool;
        private String reason;

        public static ContextToolSelection classify() {
            ContextToolSelection selection = new ContextToolSelection();
            selection.setNextTool(NextTool.CLASSIFY);
            selection.setReason("Proceed to classification");
            return selection;
        }

        public ContextToolSelection normalized() {
            ContextToolSelection selection = new ContextToolSelection();
            selection.setNextTool(nextTool != null ? nextTool : NextTool.CLASSIFY);
            selection.setReason(StringUtils.hasText(reason) ? reason.trim() : null);
            return selection;
        }
    }

    public enum NextTool {
        POLICY,
        COMMENT,
        STATUS_HISTORY,
        CLASSIFY
    }
}
