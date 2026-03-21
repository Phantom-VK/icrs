package com.college.icrs.ai.agent;

import com.college.icrs.ai.service.SentimentAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class GrievanceWorkflowNodeHandler {

    private final GrievanceAgentTools tools;

    public CompletableFuture<Map<String, Object>> loadGrievance(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        tools.loadGrievance(grievanceId);
        return CompletableFuture.completedFuture(Map.of());
    }

    public CompletableFuture<Map<String, Object>> analyzeSentiment(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        SentimentAnalysisService.SentimentDecision decision = tools.analyzeSentiment(tools.loadGrievance(grievanceId));
        Map<String, Object> updates = new HashMap<>();
        if (decision != null && decision.sentiment() != null) {
            updates.put(GrievanceAgentState.SENTIMENT, decision.sentiment().name());
        }
        if (decision != null && StringUtils.hasText(decision.modelName())) {
            updates.put(GrievanceAgentState.SENTIMENT_MODEL_NAME, decision.modelName());
        }
        return CompletableFuture.completedFuture(updates);
    }

    public CompletableFuture<Map<String, Object>> retrieveRagContext(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        String contextSection = tools.buildContextSection(
                tools.retrieveSimilar(tools.loadGrievance(grievanceId))
        );
        return CompletableFuture.completedFuture(Map.of(GrievanceAgentState.RAG_CONTEXT_SECTION, contextSection));
    }

    public CompletableFuture<Map<String, Object>> collectContext(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        ContextCollectionResult result = tools.collectContext(
                tools.loadGrievance(grievanceId),
                state.sentiment(),
                state.ragContextSection()
        );
        return CompletableFuture.completedFuture(Map.of(
                GrievanceAgentState.POLICY_CONTEXT_SECTION, result.policyContext(),
                GrievanceAgentState.POLICY_CONTEXT_FETCHED, result.policyFetched(),
                GrievanceAgentState.COMMENT_CONTEXT_SECTION, result.commentContext(),
                GrievanceAgentState.COMMENT_CONTEXT_FETCHED, result.commentFetched(),
                GrievanceAgentState.STATUS_HISTORY_CONTEXT_SECTION, result.statusHistoryContext(),
                GrievanceAgentState.STATUS_HISTORY_CONTEXT_FETCHED, result.statusHistoryFetched(),
                GrievanceAgentState.RESOLUTION_GUIDANCE_CONTEXT_SECTION, result.resolutionGuidanceContext(),
                GrievanceAgentState.RESOLUTION_GUIDANCE_CONTEXT_FETCHED, result.resolutionGuidanceFetched(),
                GrievanceAgentState.PLANNER_TRACE, result.plannerTrace(),
                GrievanceAgentState.ROUTE_TRACE, result.routeTrace()
        ));
    }

    public CompletableFuture<Map<String, Object>> classifyGrievance(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        try {
            ClassificationDecision decision = tools.classify(
                    tools.loadGrievance(grievanceId),
                    state.sentiment(),
                    state.ragContextSection(),
                    state.policyContextSection(),
                    state.commentContextSection(),
                    state.statusHistoryContextSection()
            );
            return CompletableFuture.completedFuture(classificationUpdates(decision));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Map<String, Object>> persistAiMetadata(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        tools.applyClassificationMetadata(
                grievanceId,
                state.sentiment(),
                state.sentimentModelName(),
                state.classificationPriority(),
                state.classificationAiTitle(),
                state.classificationConfidence()
        );
        return CompletableFuture.completedFuture(Map.of());
    }

    public CompletableFuture<Map<String, Object>> resolveGrievance(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        try {
            ResolutionDecision decision = tools.resolve(
                    tools.loadGrievance(grievanceId),
                    state.sentiment(),
                    state.ragContextSection(),
                    state.policyContextSection(),
                    state.commentContextSection(),
                    state.statusHistoryContextSection(),
                    state.resolutionGuidanceContextSection()
            );
            return CompletableFuture.completedFuture(resolutionUpdates(decision));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public CompletableFuture<Map<String, Object>> finalizeDecision(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        tools.finalizeDecision(
                grievanceId,
                state.sentimentModelName(),
                state.classificationConfidence(),
                state.resolutionAutoResolve(),
                state.resolutionText(),
                state.resolutionInternalComment(),
                state.resolutionConfidence()
        );
        return CompletableFuture.completedFuture(Map.of());
    }

    private Map<String, Object> classificationUpdates(ClassificationDecision decision) {
        Map<String, Object> updates = new HashMap<>();
        if (decision != null && StringUtils.hasText(decision.getPriority())) {
            updates.put(GrievanceAgentState.CLASSIFICATION_PRIORITY, decision.getPriority().trim());
        }
        if (decision != null && StringUtils.hasText(decision.getAiTitle())) {
            updates.put(GrievanceAgentState.CLASSIFICATION_AI_TITLE, decision.getAiTitle().trim());
        }
        if (decision != null && decision.getConfidence() != null) {
            updates.put(GrievanceAgentState.CLASSIFICATION_CONFIDENCE, decision.getConfidence());
        }
        return updates;
    }

    private Map<String, Object> resolutionUpdates(ResolutionDecision decision) {
        Map<String, Object> updates = new HashMap<>();
        if (decision != null && decision.getAutoResolve() != null) {
            updates.put(GrievanceAgentState.RESOLUTION_AUTO_RESOLVE, decision.getAutoResolve());
        }
        if (decision != null && StringUtils.hasText(decision.getResolutionText())) {
            updates.put(GrievanceAgentState.RESOLUTION_TEXT, decision.getResolutionText().trim());
        }
        if (decision != null && StringUtils.hasText(decision.getInternalComment())) {
            updates.put(GrievanceAgentState.RESOLUTION_INTERNAL_COMMENT, decision.getInternalComment().trim());
        }
        if (decision != null && decision.getConfidence() != null) {
            updates.put(GrievanceAgentState.RESOLUTION_CONFIDENCE, decision.getConfidence());
        }
        return updates;
    }
}
