package com.college.icrs.ai.agent;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphDefinition;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class GrievanceWorkflowGraph {

    private final GrievanceAgentTools tools;
    private final GrievanceWorkflowNodeHandler nodeHandler;
    private final CompiledGraph<GrievanceAgentState> graph;

    public GrievanceWorkflowGraph(
            GrievanceAgentTools tools,
            GrievanceWorkflowNodeHandler nodeHandler
    ) {
        this.tools = tools;
        this.nodeHandler = nodeHandler;
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
            workflow.addNode(GrievanceWorkflowNodeNames.COLLECT_CONTEXT, nodeHandler::collectContext);
            workflow.addNode(GrievanceWorkflowNodeNames.CLASSIFY_GRIEVANCE, nodeHandler::classifyGrievance);
            workflow.addNode(GrievanceWorkflowNodeNames.PERSIST_AI_METADATA, nodeHandler::persistAiMetadata);
            workflow.addNode(GrievanceWorkflowNodeNames.RESOLVE_GRIEVANCE, nodeHandler::resolveGrievance);
            workflow.addNode(GrievanceWorkflowNodeNames.FINALIZE_DECISION, nodeHandler::finalizeDecision);
            workflow.addEdge(GraphDefinition.START, GrievanceWorkflowNodeNames.LOAD_GRIEVANCE);
            workflow.addEdge(GrievanceWorkflowNodeNames.LOAD_GRIEVANCE, GrievanceWorkflowNodeNames.ANALYZE_SENTIMENT);
            workflow.addEdge(GrievanceWorkflowNodeNames.ANALYZE_SENTIMENT, GrievanceWorkflowNodeNames.RETRIEVE_RAG_CONTEXT);
            workflow.addEdge(GrievanceWorkflowNodeNames.RETRIEVE_RAG_CONTEXT, GrievanceWorkflowNodeNames.COLLECT_CONTEXT);
            workflow.addEdge(GrievanceWorkflowNodeNames.COLLECT_CONTEXT, GrievanceWorkflowNodeNames.CLASSIFY_GRIEVANCE);
            workflow.addEdge(GrievanceWorkflowNodeNames.CLASSIFY_GRIEVANCE, GrievanceWorkflowNodeNames.PERSIST_AI_METADATA);
            workflow.addEdge(GrievanceWorkflowNodeNames.PERSIST_AI_METADATA, GrievanceWorkflowNodeNames.RESOLVE_GRIEVANCE);
            workflow.addEdge(GrievanceWorkflowNodeNames.RESOLVE_GRIEVANCE, GrievanceWorkflowNodeNames.FINALIZE_DECISION);
            workflow.addEdge(GrievanceWorkflowNodeNames.FINALIZE_DECISION, GraphDefinition.END);
            return workflow.compile();
        } catch (GraphStateException e) {
            throw new IllegalStateException("Failed to initialize grievance workflow graph", e);
        }
    }
}
