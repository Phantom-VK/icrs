package com.college.icrs.ai.agent;

import com.college.icrs.model.Sentiment;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.util.StringUtils;

import java.util.Map;

public class GrievanceAgentState extends AgentState {

    public static final String GRIEVANCE_ID = "grievanceId";
    public static final String SENTIMENT = "sentiment";
    public static final String SENTIMENT_MODEL_NAME = "sentimentModelName";
    public static final String RAG_CONTEXT_SECTION = "ragContextSection";
    public static final String NEXT_TOOL = "nextTool";
    public static final String NEXT_TOOL_REASON = "nextToolReason";
    public static final String PLANNER_ITERATION = "plannerIteration";
    public static final String PLANNER_TRACE = "plannerTrace";
    public static final String ROUTE_TRACE = "routeTrace";
    public static final String POLICY_CONTEXT_SECTION = "policyContextSection";
    public static final String POLICY_CONTEXT_FETCHED = "policyContextFetched";
    public static final String COMMENT_CONTEXT_SECTION = "commentContextSection";
    public static final String COMMENT_CONTEXT_FETCHED = "commentContextFetched";
    public static final String STATUS_HISTORY_CONTEXT_SECTION = "statusHistoryContextSection";
    public static final String STATUS_HISTORY_CONTEXT_FETCHED = "statusHistoryContextFetched";
    public static final String CLASSIFICATION_PRIORITY = "classificationPriority";
    public static final String CLASSIFICATION_AI_TITLE = "classificationAiTitle";
    public static final String CLASSIFICATION_CONFIDENCE = "classificationConfidence";
    public static final String RESOLUTION_AUTO_RESOLVE = "resolutionAutoResolve";
    public static final String RESOLUTION_TEXT = "resolutionText";
    public static final String RESOLUTION_INTERNAL_COMMENT = "resolutionInternalComment";
    public static final String RESOLUTION_CONFIDENCE = "resolutionConfidence";

    public GrievanceAgentState(Map<String, Object> data) {
        super(data);
    }

    public Long grievanceId() {
        return value(GRIEVANCE_ID).map(Long.class::cast).orElse(null);
    }

    public Sentiment sentiment() {
        String value = value(SENTIMENT).map(String.class::cast).orElse(null);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Sentiment.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String sentimentModelName() {
        return value(SENTIMENT_MODEL_NAME).map(String.class::cast).orElse(null);
    }

    public String ragContextSection() {
        return value(RAG_CONTEXT_SECTION).map(String.class::cast).orElse("");
    }

    public NextTool nextTool() {
        String value = value(NEXT_TOOL).map(String.class::cast).orElse(null);
        if (!StringUtils.hasText(value)) {
            return NextTool.CLASSIFY;
        }
        try {
            return NextTool.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return NextTool.CLASSIFY;
        }
    }

    public String nextToolReason() {
        return value(NEXT_TOOL_REASON).map(String.class::cast).orElse(null);
    }

    public int plannerIteration() {
        return value(PLANNER_ITERATION).map(Integer.class::cast).orElse(0);
    }

    public String plannerTrace() {
        return value(PLANNER_TRACE).map(String.class::cast).orElse("");
    }

    public String routeTrace() {
        return value(ROUTE_TRACE).map(String.class::cast).orElse("");
    }

    public String policyContextSection() {
        return value(POLICY_CONTEXT_SECTION).map(String.class::cast).orElse("");
    }

    public boolean policyContextFetched() {
        return value(POLICY_CONTEXT_FETCHED).map(Boolean.class::cast).orElse(false);
    }

    public String commentContextSection() {
        return value(COMMENT_CONTEXT_SECTION).map(String.class::cast).orElse("");
    }

    public boolean commentContextFetched() {
        return value(COMMENT_CONTEXT_FETCHED).map(Boolean.class::cast).orElse(false);
    }

    public String statusHistoryContextSection() {
        return value(STATUS_HISTORY_CONTEXT_SECTION).map(String.class::cast).orElse("");
    }

    public boolean statusHistoryContextFetched() {
        return value(STATUS_HISTORY_CONTEXT_FETCHED).map(Boolean.class::cast).orElse(false);
    }

    public String classificationPriority() {
        return value(CLASSIFICATION_PRIORITY).map(String.class::cast).orElse(null);
    }

    public String classificationAiTitle() {
        return value(CLASSIFICATION_AI_TITLE).map(String.class::cast).orElse(null);
    }

    public Double classificationConfidence() {
        return value(CLASSIFICATION_CONFIDENCE).map(Double.class::cast).orElse(null);
    }

    public Boolean resolutionAutoResolve() {
        return value(RESOLUTION_AUTO_RESOLVE).map(Boolean.class::cast).orElse(null);
    }

    public String resolutionText() {
        return value(RESOLUTION_TEXT).map(String.class::cast).orElse(null);
    }

    public String resolutionInternalComment() {
        return value(RESOLUTION_INTERNAL_COMMENT).map(String.class::cast).orElse(null);
    }

    public Double resolutionConfidence() {
        return value(RESOLUTION_CONFIDENCE).map(Double.class::cast).orElse(null);
    }
}
