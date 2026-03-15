package com.college.icrs.ai.agent;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class GrievanceWorkflowGraph {

    private final GrievanceAgentTools tools;
    private final GrievanceWorkflowNodeHandler nodeHandler;
    private final GrievanceWorkflowRoutingService routingService;
    private final CompiledGraph<GrievanceAgentState> graph;

    public GrievanceWorkflowGraph(
            GrievanceAgentTools tools,
            GrievanceWorkflowNodeHandler nodeHandler,
            GrievanceWorkflowRoutingService routingService
    ) {
        this.tools = tools;
        this.nodeHandler = nodeHandler;
        this.routingService = routingService;
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
            workflow.addNode(GrievanceWorkflowNodeNames.LOAD_GRIEVANCE, nodeHandler::loadGrievance);
            workflow.addNode(GrievanceWorkflowNodeNames.ANALYZE_SENTIMENT, nodeHandler::analyzeSentiment);
            workflow.addNode(GrievanceWorkflowNodeNames.RETRIEVE_RAG_CONTEXT, nodeHandler::retrieveRagContext);
            workflow.addNode(GrievanceWorkflowNodeNames.PLAN_CONTEXT_TOOLS, nodeHandler::planContextTools);
            workflow.addNode(GrievanceWorkflowNodeNames.LOAD_POLICY_CONTEXT, nodeHandler::loadPolicyContext);
            workflow.addNode(GrievanceWorkflowNodeNames.LOAD_COMMENT_CONTEXT, nodeHandler::loadCommentContext);
            workflow.addNode(GrievanceWorkflowNodeNames.LOAD_STATUS_HISTORY_CONTEXT, nodeHandler::loadStatusHistoryContext);
            workflow.addNode(GrievanceWorkflowNodeNames.CLASSIFY_GRIEVANCE, nodeHandler::classifyGrievance);
            workflow.addNode(GrievanceWorkflowNodeNames.PERSIST_AI_METADATA, nodeHandler::persistAiMetadata);
            workflow.addNode(GrievanceWorkflowNodeNames.RESOLVE_GRIEVANCE, nodeHandler::resolveGrievance);
            workflow.addNode(GrievanceWorkflowNodeNames.FINALIZE_DECISION, nodeHandler::finalizeDecision);
            workflow.addEdge(GraphDefinition.START, GrievanceWorkflowNodeNames.LOAD_GRIEVANCE);
            workflow.addEdge(GrievanceWorkflowNodeNames.LOAD_GRIEVANCE, GrievanceWorkflowNodeNames.ANALYZE_SENTIMENT);
            workflow.addEdge(GrievanceWorkflowNodeNames.ANALYZE_SENTIMENT, GrievanceWorkflowNodeNames.RETRIEVE_RAG_CONTEXT);
            workflow.addEdge(GrievanceWorkflowNodeNames.RETRIEVE_RAG_CONTEXT, GrievanceWorkflowNodeNames.PLAN_CONTEXT_TOOLS);
            workflow.addConditionalEdges(GrievanceWorkflowNodeNames.PLAN_CONTEXT_TOOLS, this::routeAfterPlanner, routingService.routeMap());
            workflow.addEdge(GrievanceWorkflowNodeNames.LOAD_POLICY_CONTEXT, GrievanceWorkflowNodeNames.PLAN_CONTEXT_TOOLS);
            workflow.addEdge(GrievanceWorkflowNodeNames.LOAD_COMMENT_CONTEXT, GrievanceWorkflowNodeNames.PLAN_CONTEXT_TOOLS);
            workflow.addEdge(GrievanceWorkflowNodeNames.LOAD_STATUS_HISTORY_CONTEXT, GrievanceWorkflowNodeNames.PLAN_CONTEXT_TOOLS);
            workflow.addEdge(GrievanceWorkflowNodeNames.CLASSIFY_GRIEVANCE, GrievanceWorkflowNodeNames.PERSIST_AI_METADATA);
            workflow.addEdge(GrievanceWorkflowNodeNames.PERSIST_AI_METADATA, GrievanceWorkflowNodeNames.RESOLVE_GRIEVANCE);
            workflow.addEdge(GrievanceWorkflowNodeNames.RESOLVE_GRIEVANCE, GrievanceWorkflowNodeNames.FINALIZE_DECISION);
            workflow.addEdge(GrievanceWorkflowNodeNames.FINALIZE_DECISION, GraphDefinition.END);
            return workflow.compile();
        } catch (GraphStateException e) {
            throw new IllegalStateException("Failed to initialize grievance workflow graph", e);
        }
    }

    private CompletableFuture<String> routeAfterPlanner(GrievanceAgentState state) {
        return CompletableFuture.completedFuture(routingService.nextContextRoute(state));
    }
}
