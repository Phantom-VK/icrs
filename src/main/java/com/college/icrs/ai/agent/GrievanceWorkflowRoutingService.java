package com.college.icrs.ai.agent;

import com.college.icrs.logging.IcrsLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@Slf4j
public class GrievanceWorkflowRoutingService {

    private static final int MAX_PLANNER_ITERATIONS = 4;

    public String nextContextRoute(GrievanceAgentState state) {
        if (state.plannerIteration() >= MAX_PLANNER_ITERATIONS) {
            log.info(IcrsLog.event("ai.context-router.decision",
                    "grievanceId", state.grievanceId(),
                    "iteration", state.plannerIteration(),
                    "nextRoute", GrievanceWorkflowNodeNames.ROUTE_CLASSIFY,
                    "reason", "max-iterations"));
            return GrievanceWorkflowNodeNames.ROUTE_CLASSIFY;
        }

        NextTool nextTool = state.nextTool();
        if (nextTool == NextTool.POLICY && !state.policyContextFetched()) {
            return logAndReturnRoute(state, GrievanceWorkflowNodeNames.ROUTE_POLICY);
        }
        if (nextTool == NextTool.COMMENT && !state.commentContextFetched()) {
            return logAndReturnRoute(state, GrievanceWorkflowNodeNames.ROUTE_COMMENT);
        }
        if (nextTool == NextTool.STATUS_HISTORY && !state.statusHistoryContextFetched()) {
            return logAndReturnRoute(state, GrievanceWorkflowNodeNames.ROUTE_HISTORY);
        }
        return logAndReturnRoute(state, GrievanceWorkflowNodeNames.ROUTE_CLASSIFY);
    }

    public Map<String, String> routeMap() {
        return Map.of(
                GrievanceWorkflowNodeNames.ROUTE_POLICY, GrievanceWorkflowNodeNames.LOAD_POLICY_CONTEXT,
                GrievanceWorkflowNodeNames.ROUTE_COMMENT, GrievanceWorkflowNodeNames.LOAD_COMMENT_CONTEXT,
                GrievanceWorkflowNodeNames.ROUTE_HISTORY, GrievanceWorkflowNodeNames.LOAD_STATUS_HISTORY_CONTEXT,
                GrievanceWorkflowNodeNames.ROUTE_CLASSIFY, GrievanceWorkflowNodeNames.CLASSIFY_GRIEVANCE
        );
    }

    public String appendTrace(String current, String next) {
        if (!StringUtils.hasText(next)) {
            return current;
        }
        if (!StringUtils.hasText(current)) {
            return next;
        }
        return current + " -> " + next;
    }

    public String compact(String value) {
        if (!StringUtils.hasText(value)) {
            return "n/a";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String logAndReturnRoute(GrievanceAgentState state, String route) {
        log.info(IcrsLog.event("ai.context-router.decision",
                "grievanceId", state.grievanceId(),
                "iteration", state.plannerIteration(),
                "nextRoute", route,
                "reason", compact(state.nextToolReason())));
        return route;
    }
}
