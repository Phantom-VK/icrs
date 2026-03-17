package com.college.icrs.ai.agent;

import com.college.icrs.ai.service.SentimentAnalysisService;
import com.college.icrs.logging.IcrsLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class GrievanceWorkflowNodeHandler {

    private final GrievanceAgentTools tools;
    private final GrievanceWorkflowRoutingService routingService;

    public CompletableFuture<Map<String, Object>> loadGrievance(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        logNodeStart(GrievanceWorkflowNodeNames.LOAD_GRIEVANCE, grievanceId);
        tools.loadGrievance(grievanceId);
        return CompletableFuture.completedFuture(Map.of());
    }

    public CompletableFuture<Map<String, Object>> analyzeSentiment(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        logNodeStart(GrievanceWorkflowNodeNames.ANALYZE_SENTIMENT, grievanceId);
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
        logNodeStart(GrievanceWorkflowNodeNames.RETRIEVE_RAG_CONTEXT, grievanceId);
        String contextSection = tools.buildContextSection(
                tools.retrieveSimilar(tools.loadGrievance(grievanceId))
        );
        return CompletableFuture.completedFuture(Map.of(GrievanceAgentState.RAG_CONTEXT_SECTION, contextSection));
    }

    public CompletableFuture<Map<String, Object>> planContextTools(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        int nextIteration = state.plannerIteration() + 1;
        logNodeStart(GrievanceWorkflowNodeNames.PLAN_CONTEXT_TOOLS, grievanceId);
        ContextToolSelection selection = tools.selectContextTools(
                tools.loadGrievance(grievanceId),
                state.sentiment(),
                state.ragContextSection(),
                state.policyContextSection(),
                state.commentContextSection(),
                state.statusHistoryContextSection(),
                state.resolutionGuidanceContextSection(),
                state.policyContextFetched(),
                state.commentContextFetched(),
                state.statusHistoryContextFetched(),
                state.resolutionGuidanceContextFetched(),
                nextIteration
        );
        String nextTool = selection.getNextTool() != null ? selection.getNextTool().name() : NextTool.CLASSIFY.name();
        String compactReason = routingService.compact(selection.getReason());
        String plannerTrace = routingService.appendTrace(state.plannerTrace(), nextIteration + ":" + nextTool + "(" + compactReason + ")");
        log.info(IcrsLog.event("ai.context-planner.decision",
                "grievanceId", grievanceId,
                "iteration", nextIteration,
                "nextTool", nextTool,
                "reason", compactReason,
                "policyFetched", state.policyContextFetched(),
                "commentFetched", state.commentContextFetched(),
                "statusHistoryFetched", state.statusHistoryContextFetched(),
                "resolutionGuidanceFetched", state.resolutionGuidanceContextFetched()));
        return CompletableFuture.completedFuture(Map.of(
                GrievanceAgentState.NEXT_TOOL, nextTool,
                GrievanceAgentState.NEXT_TOOL_REASON, compactReason,
                GrievanceAgentState.PLANNER_ITERATION, nextIteration,
                GrievanceAgentState.PLANNER_TRACE, plannerTrace
        ));
    }

    public CompletableFuture<Map<String, Object>> loadPolicyContext(GrievanceAgentState state) {
        return fetchContextTool(
                state,
                GrievanceWorkflowNodeNames.LOAD_POLICY_CONTEXT,
                "POLICY",
                GrievanceAgentState.POLICY_CONTEXT_SECTION,
                GrievanceAgentState.POLICY_CONTEXT_FETCHED,
                tools.buildPolicyContext(state.grievanceId())
        );
    }

    public CompletableFuture<Map<String, Object>> loadCommentContext(GrievanceAgentState state) {
        return fetchContextTool(
                state,
                GrievanceWorkflowNodeNames.LOAD_COMMENT_CONTEXT,
                "COMMENT",
                GrievanceAgentState.COMMENT_CONTEXT_SECTION,
                GrievanceAgentState.COMMENT_CONTEXT_FETCHED,
                tools.buildCommentContext(state.grievanceId())
        );
    }

    public CompletableFuture<Map<String, Object>> loadStatusHistoryContext(GrievanceAgentState state) {
        return fetchContextTool(
                state,
                GrievanceWorkflowNodeNames.LOAD_STATUS_HISTORY_CONTEXT,
                "STATUS_HISTORY",
                GrievanceAgentState.STATUS_HISTORY_CONTEXT_SECTION,
                GrievanceAgentState.STATUS_HISTORY_CONTEXT_FETCHED,
                tools.buildStatusHistoryContext(state.grievanceId())
        );
    }

    public CompletableFuture<Map<String, Object>> loadResolutionGuidanceContext(GrievanceAgentState state) {
        return fetchContextTool(
                state,
                GrievanceWorkflowNodeNames.LOAD_RESOLUTION_GUIDANCE_CONTEXT,
                "RESOLUTION_GUIDANCE",
                GrievanceAgentState.RESOLUTION_GUIDANCE_CONTEXT_SECTION,
                GrievanceAgentState.RESOLUTION_GUIDANCE_CONTEXT_FETCHED,
                tools.buildResolutionGuidanceContext(state.grievanceId())
        );
    }

    public CompletableFuture<Map<String, Object>> classifyGrievance(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        logNodeStart(GrievanceWorkflowNodeNames.CLASSIFY_GRIEVANCE, grievanceId);
        log.info(IcrsLog.event("ai.context-telemetry.pre-classification",
                "grievanceId", grievanceId,
                "plannerIterations", state.plannerIteration(),
                "routeTrace", routingService.appendTrace(state.routeTrace(), "CLASSIFY"),
                "plannerTrace", state.plannerTrace()));
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
        logNodeStart(GrievanceWorkflowNodeNames.PERSIST_AI_METADATA, grievanceId);
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
        logNodeStart(GrievanceWorkflowNodeNames.RESOLVE_GRIEVANCE, grievanceId);
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
        logNodeStart(GrievanceWorkflowNodeNames.FINALIZE_DECISION, grievanceId);
        log.info(IcrsLog.event("ai.context-telemetry.summary",
                "grievanceId", grievanceId,
                "plannerIterations", state.plannerIteration(),
                "routeTrace", routingService.appendTrace(state.routeTrace(), "CLASSIFY"),
                "plannerTrace", state.plannerTrace(),
                "policyFetched", state.policyContextFetched(),
                "commentFetched", state.commentContextFetched(),
                "statusHistoryFetched", state.statusHistoryContextFetched(),
                "resolutionGuidanceFetched", state.resolutionGuidanceContextFetched()));
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

    private CompletableFuture<Map<String, Object>> fetchContextTool(
            GrievanceAgentState state,
            String nodeName,
            String toolName,
            String contentKey,
            String fetchedKey,
            String content
    ) {
        Long grievanceId = state.grievanceId();
        logNodeStart(nodeName, grievanceId);
        String routeTrace = routingService.appendTrace(state.routeTrace(), toolName);
        log.info(IcrsLog.event("ai.context-tool.fetched",
                "grievanceId", grievanceId,
                "tool", toolName,
                "iteration", state.plannerIteration(),
                "routeTrace", routeTrace));
        return CompletableFuture.completedFuture(Map.of(
                contentKey, content,
                fetchedKey, true,
                GrievanceAgentState.ROUTE_TRACE, routeTrace
        ));
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

    private void logNodeStart(String nodeName, Long grievanceId) {
        log.info(IcrsLog.event("ai.graph.node.start", "node", nodeName, "grievanceId", grievanceId));
    }
}
