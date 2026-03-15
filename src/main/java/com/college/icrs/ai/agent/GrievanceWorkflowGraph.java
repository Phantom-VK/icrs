package com.college.icrs.ai.agent;

import com.college.icrs.ai.service.SentimentAnalysisService;
import com.college.icrs.logging.IcrsLog;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class GrievanceWorkflowGraph {

    private static final int MAX_PLANNER_ITERATIONS = 4;
    private static final String ROUTE_POLICY = "route-policy";
    private static final String ROUTE_COMMENT = "route-comment";
    private static final String ROUTE_HISTORY = "route-history";
    private static final String ROUTE_CLASSIFY = "route-classify";

    private static final String LOAD_GRIEVANCE = "load-grievance";
    private static final String ANALYZE_SENTIMENT = "analyze-sentiment";
    private static final String RETRIEVE_RAG_CONTEXT = "retrieve-rag-context";
    private static final String PLAN_CONTEXT_TOOLS = "plan-context-tools";
    private static final String LOAD_POLICY_CONTEXT = "load-policy-context";
    private static final String LOAD_COMMENT_CONTEXT = "load-comment-context";
    private static final String LOAD_STATUS_HISTORY_CONTEXT = "load-status-history-context";
    private static final String CLASSIFY_GRIEVANCE = "classify-grievance";
    private static final String PERSIST_AI_METADATA = "persist-ai-metadata";
    private static final String RESOLVE_GRIEVANCE = "resolve-grievance";
    private static final String FINALIZE_DECISION = "finalize-decision";

    private final GrievanceAgentTools tools;
    private final CompiledGraph<GrievanceAgentState> graph;

    public GrievanceWorkflowGraph(GrievanceAgentTools tools) {
        this.tools = tools;
        this.graph = compileGraph();
    }

    public com.college.icrs.model.Grievance process(Long grievanceId) {
        Optional<GrievanceAgentState> result = graph.invoke(Map.of(GrievanceAgentState.GRIEVANCE_ID, grievanceId));
        if (result.isEmpty()) {
            throw new IllegalStateException("Workflow graph returned no grievance state");
        }
        return tools.loadGrievance(grievanceId);
    }

    private CompiledGraph<GrievanceAgentState> compileGraph() {
        try {
            StateGraph<GrievanceAgentState> workflow = new StateGraph<>(GrievanceAgentState::new);
            workflow.addNode(LOAD_GRIEVANCE, this::loadGrievance);
            workflow.addNode(ANALYZE_SENTIMENT, this::analyzeSentiment);
            workflow.addNode(RETRIEVE_RAG_CONTEXT, this::retrieveRagContext);
            workflow.addNode(PLAN_CONTEXT_TOOLS, this::planContextTools);
            workflow.addNode(LOAD_POLICY_CONTEXT, this::loadPolicyContext);
            workflow.addNode(LOAD_COMMENT_CONTEXT, this::loadCommentContext);
            workflow.addNode(LOAD_STATUS_HISTORY_CONTEXT, this::loadStatusHistoryContext);
            workflow.addNode(CLASSIFY_GRIEVANCE, this::classifyGrievance);
            workflow.addNode(PERSIST_AI_METADATA, this::persistAiMetadata);
            workflow.addNode(RESOLVE_GRIEVANCE, this::resolveGrievance);
            workflow.addNode(FINALIZE_DECISION, this::finalizeDecision);
            workflow.addEdge(GraphDefinition.START, LOAD_GRIEVANCE);
            workflow.addEdge(LOAD_GRIEVANCE, ANALYZE_SENTIMENT);
            workflow.addEdge(ANALYZE_SENTIMENT, RETRIEVE_RAG_CONTEXT);
            workflow.addEdge(RETRIEVE_RAG_CONTEXT, PLAN_CONTEXT_TOOLS);
            workflow.addConditionalEdges(PLAN_CONTEXT_TOOLS, this::routeAfterPlanner, routeMap());
            workflow.addEdge(LOAD_POLICY_CONTEXT, PLAN_CONTEXT_TOOLS);
            workflow.addEdge(LOAD_COMMENT_CONTEXT, PLAN_CONTEXT_TOOLS);
            workflow.addEdge(LOAD_STATUS_HISTORY_CONTEXT, PLAN_CONTEXT_TOOLS);
            workflow.addEdge(CLASSIFY_GRIEVANCE, PERSIST_AI_METADATA);
            workflow.addEdge(PERSIST_AI_METADATA, RESOLVE_GRIEVANCE);
            workflow.addEdge(RESOLVE_GRIEVANCE, FINALIZE_DECISION);
            workflow.addEdge(FINALIZE_DECISION, GraphDefinition.END);
            return workflow.compile();
        } catch (GraphStateException e) {
            throw new IllegalStateException("Failed to initialize grievance workflow graph", e);
        }
    }

    private CompletableFuture<Map<String, Object>> loadGrievance(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        log.info(IcrsLog.event("ai.graph.node.start", "node", LOAD_GRIEVANCE, "grievanceId", grievanceId));
        tools.loadGrievance(grievanceId);
        return CompletableFuture.completedFuture(Map.of());
    }

    private CompletableFuture<Map<String, Object>> analyzeSentiment(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        log.info(IcrsLog.event("ai.graph.node.start", "node", ANALYZE_SENTIMENT, "grievanceId", grievanceId));
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

    private CompletableFuture<Map<String, Object>> retrieveRagContext(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        log.info(IcrsLog.event("ai.graph.node.start", "node", RETRIEVE_RAG_CONTEXT, "grievanceId", grievanceId));
        String contextSection = tools.buildContextSection(
                tools.retrieveSimilar(tools.loadGrievance(grievanceId))
        );
        return CompletableFuture.completedFuture(Map.of(GrievanceAgentState.RAG_CONTEXT_SECTION, contextSection));
    }

    private CompletableFuture<Map<String, Object>> planContextTools(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        int nextIteration = state.plannerIteration() + 1;
        log.info(IcrsLog.event("ai.graph.node.start", "node", PLAN_CONTEXT_TOOLS, "grievanceId", grievanceId));
        ContextToolSelection selection = tools.selectContextTools(
                tools.loadGrievance(grievanceId),
                state.sentiment(),
                state.ragContextSection(),
                state.policyContextSection(),
                state.commentContextSection(),
                state.statusHistoryContextSection(),
                state.policyContextFetched(),
                state.commentContextFetched(),
                state.statusHistoryContextFetched(),
                nextIteration
        );
        String nextTool = selection.getNextTool() != null ? selection.getNextTool().name() : NextTool.CLASSIFY.name();
        String plannerTrace = appendTrace(state.plannerTrace(), nextIteration + ":" + nextTool + "(" + compact(selection.getReason()) + ")");
        log.info(IcrsLog.event("ai.context-planner.decision",
                "grievanceId", grievanceId,
                "iteration", nextIteration,
                "nextTool", nextTool,
                "reason", compact(selection.getReason()),
                "policyFetched", state.policyContextFetched(),
                "commentFetched", state.commentContextFetched(),
                "statusHistoryFetched", state.statusHistoryContextFetched()));
        return CompletableFuture.completedFuture(Map.of(
                GrievanceAgentState.NEXT_TOOL,
                nextTool,
                GrievanceAgentState.NEXT_TOOL_REASON,
                compact(selection.getReason()),
                GrievanceAgentState.PLANNER_ITERATION,
                nextIteration,
                GrievanceAgentState.PLANNER_TRACE,
                plannerTrace
        ));
    }

    private CompletableFuture<Map<String, Object>> loadPolicyContext(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        log.info(IcrsLog.event("ai.graph.node.start", "node", LOAD_POLICY_CONTEXT, "grievanceId", grievanceId));
        String routeTrace = appendTrace(state.routeTrace(), "POLICY");
        log.info(IcrsLog.event("ai.context-tool.fetched",
                "grievanceId", grievanceId,
                "tool", "POLICY",
                "iteration", state.plannerIteration(),
                "routeTrace", routeTrace));
        return CompletableFuture.completedFuture(Map.of(
                GrievanceAgentState.POLICY_CONTEXT_SECTION,
                tools.buildPolicyContext(grievanceId),
                GrievanceAgentState.POLICY_CONTEXT_FETCHED,
                true,
                GrievanceAgentState.ROUTE_TRACE,
                routeTrace
        ));
    }

    private CompletableFuture<Map<String, Object>> loadCommentContext(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        log.info(IcrsLog.event("ai.graph.node.start", "node", LOAD_COMMENT_CONTEXT, "grievanceId", grievanceId));
        String routeTrace = appendTrace(state.routeTrace(), "COMMENT");
        log.info(IcrsLog.event("ai.context-tool.fetched",
                "grievanceId", grievanceId,
                "tool", "COMMENT",
                "iteration", state.plannerIteration(),
                "routeTrace", routeTrace));
        return CompletableFuture.completedFuture(Map.of(
                GrievanceAgentState.COMMENT_CONTEXT_SECTION,
                tools.buildCommentContext(grievanceId),
                GrievanceAgentState.COMMENT_CONTEXT_FETCHED,
                true,
                GrievanceAgentState.ROUTE_TRACE,
                routeTrace
        ));
    }

    private CompletableFuture<Map<String, Object>> loadStatusHistoryContext(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        log.info(IcrsLog.event("ai.graph.node.start", "node", LOAD_STATUS_HISTORY_CONTEXT, "grievanceId", grievanceId));
        String routeTrace = appendTrace(state.routeTrace(), "STATUS_HISTORY");
        log.info(IcrsLog.event("ai.context-tool.fetched",
                "grievanceId", grievanceId,
                "tool", "STATUS_HISTORY",
                "iteration", state.plannerIteration(),
                "routeTrace", routeTrace));
        return CompletableFuture.completedFuture(Map.of(
                GrievanceAgentState.STATUS_HISTORY_CONTEXT_SECTION,
                tools.buildStatusHistoryContext(grievanceId),
                GrievanceAgentState.STATUS_HISTORY_CONTEXT_FETCHED,
                true,
                GrievanceAgentState.ROUTE_TRACE,
                routeTrace
        ));
    }

    private CompletableFuture<Map<String, Object>> classifyGrievance(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        log.info(IcrsLog.event("ai.graph.node.start", "node", CLASSIFY_GRIEVANCE, "grievanceId", grievanceId));
        log.info(IcrsLog.event("ai.context-telemetry.pre-classification",
                "grievanceId", grievanceId,
                "plannerIterations", state.plannerIteration(),
                "routeTrace", appendTrace(state.routeTrace(), "CLASSIFY"),
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
            return CompletableFuture.completedFuture(updates);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Map<String, Object>> persistAiMetadata(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        log.info(IcrsLog.event("ai.graph.node.start", "node", PERSIST_AI_METADATA, "grievanceId", grievanceId));
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

    private CompletableFuture<Map<String, Object>> resolveGrievance(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        log.info(IcrsLog.event("ai.graph.node.start", "node", RESOLVE_GRIEVANCE, "grievanceId", grievanceId));
        try {
            ResolutionDecision decision = tools.resolve(
                    tools.loadGrievance(grievanceId),
                    state.sentiment(),
                    state.ragContextSection(),
                    state.policyContextSection(),
                    state.commentContextSection(),
                    state.statusHistoryContextSection()
            );
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
            return CompletableFuture.completedFuture(updates);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Map<String, Object>> finalizeDecision(GrievanceAgentState state) {
        Long grievanceId = state.grievanceId();
        log.info(IcrsLog.event("ai.graph.node.start", "node", FINALIZE_DECISION, "grievanceId", grievanceId));
        log.info(IcrsLog.event("ai.context-telemetry.summary",
                "grievanceId", grievanceId,
                "plannerIterations", state.plannerIteration(),
                "routeTrace", appendTrace(state.routeTrace(), "CLASSIFY"),
                "plannerTrace", state.plannerTrace(),
                "policyFetched", state.policyContextFetched(),
                "commentFetched", state.commentContextFetched(),
                "statusHistoryFetched", state.statusHistoryContextFetched()));
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

    private CompletableFuture<String> routeAfterPlanner(GrievanceAgentState state) {
        return CompletableFuture.completedFuture(nextContextRoute(state));
    }

    private String nextContextRoute(GrievanceAgentState state) {
        if (state.plannerIteration() >= MAX_PLANNER_ITERATIONS) {
            log.info(IcrsLog.event("ai.context-router.decision",
                    "grievanceId", state.grievanceId(),
                    "iteration", state.plannerIteration(),
                    "nextRoute", ROUTE_CLASSIFY,
                    "reason", "max-iterations"));
            return ROUTE_CLASSIFY;
        }

        NextTool nextTool = state.nextTool();
        if (nextTool == NextTool.POLICY && !state.policyContextFetched()) {
            log.info(IcrsLog.event("ai.context-router.decision",
                    "grievanceId", state.grievanceId(),
                    "iteration", state.plannerIteration(),
                    "nextRoute", ROUTE_POLICY,
                    "reason", compact(state.nextToolReason())));
            return ROUTE_POLICY;
        }
        if (nextTool == NextTool.COMMENT && !state.commentContextFetched()) {
            log.info(IcrsLog.event("ai.context-router.decision",
                    "grievanceId", state.grievanceId(),
                    "iteration", state.plannerIteration(),
                    "nextRoute", ROUTE_COMMENT,
                    "reason", compact(state.nextToolReason())));
            return ROUTE_COMMENT;
        }
        if (nextTool == NextTool.STATUS_HISTORY && !state.statusHistoryContextFetched()) {
            log.info(IcrsLog.event("ai.context-router.decision",
                    "grievanceId", state.grievanceId(),
                    "iteration", state.plannerIteration(),
                    "nextRoute", ROUTE_HISTORY,
                    "reason", compact(state.nextToolReason())));
            return ROUTE_HISTORY;
        }
        log.info(IcrsLog.event("ai.context-router.decision",
                "grievanceId", state.grievanceId(),
                "iteration", state.plannerIteration(),
                "nextRoute", ROUTE_CLASSIFY,
                "reason", compact(state.nextToolReason())));
        return ROUTE_CLASSIFY;
    }

    private Map<String, String> routeMap() {
        return Map.of(
                ROUTE_POLICY, LOAD_POLICY_CONTEXT,
                ROUTE_COMMENT, LOAD_COMMENT_CONTEXT,
                ROUTE_HISTORY, LOAD_STATUS_HISTORY_CONTEXT,
                ROUTE_CLASSIFY, CLASSIFY_GRIEVANCE
        );
    }

    private String appendTrace(String current, String next) {
        if (!StringUtils.hasText(next)) {
            return current;
        }
        if (!StringUtils.hasText(current)) {
            return next;
        }
        return current + " -> " + next;
    }

    private String compact(String value) {
        if (!StringUtils.hasText(value)) {
            return "n/a";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
