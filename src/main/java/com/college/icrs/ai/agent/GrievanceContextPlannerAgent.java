package com.college.icrs.ai.agent;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Sentiment;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.invocation.InvocationContext;
import dev.langchain4j.invocation.InvocationParameters;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ToolChoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.langchain4j.tool.LC4jToolService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceContextPlannerAgent {

    private static final int MAX_TOOL_CALL_ROUNDS = 5;

    private final ChatModel chatModel;
    private final IcrsProperties icrsProperties;
    private final GrievanceAgentContextService contextService;

    public ContextCollectionResult collectContext(
            Grievance grievance,
            Sentiment sentiment,
            String ragContext
    ) {
        if (grievance == null) {
            return ContextCollectionResult.empty();
        }

        ContextPlannerToolSet toolSet = new ContextPlannerToolSet(grievance.getId(), contextService);
        LC4jToolService toolService = LC4jToolService.builder()
                .specification(toolSet)
                .build();
        Set<String> usedTools = new LinkedHashSet<>();
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt()));
        messages.add(UserMessage.from(userPrompt(grievance, sentiment, ragContext)));

        String plannerTrace = "";
        String routeTrace = "";

        for (int round = 1; round <= MAX_TOOL_CALL_ROUNDS; round++) {
            var availableSpecifications = availableSpecifications(toolService, usedTools);
            ChatRequest request = ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(availableSpecifications)
                    .toolChoice(availableSpecifications.isEmpty() ? ToolChoice.NONE : ToolChoice.AUTO)
                    .build();

            AiMessage aiMessage = chatModel.chat(request).aiMessage();
            messages.add(aiMessage);

            if (!aiMessage.hasToolExecutionRequests()) {
                plannerTrace = appendTrace(plannerTrace, compact(aiMessage.text()));
                break;
            }

            List<ToolExecutionRequest> requests = aiMessage.toolExecutionRequests().stream()
                    .filter(requested -> usedTools.add(requested.name()))
                    .toList();

            if (requests.isEmpty()) {
                plannerTrace = appendTrace(plannerTrace, "No new tools requested");
                break;
            }

            routeTrace = appendTrace(routeTrace, requests.stream().map(ToolExecutionRequest::name).reduce((a, b) -> a + " -> " + b).orElse(""));
            plannerTrace = appendTrace(plannerTrace, "round-" + round + ": " + routeTraceSegment(requests));
            int currentRound = round;
            String currentRouteTrace = routeTrace;

            List<ToolExecutionResultMessage> results = executeTools(toolService, requests);
            results.forEach(result -> log.info(IcrsLog.event("ai.context-tool.fetched",
                    "grievanceId", grievance.getId(),
                    "tool", result.toolName(),
                    "iteration", currentRound,
                    "routeTrace", currentRouteTrace)));
            messages.addAll(results);
        }

        if (!usedTools.isEmpty()) {
            log.info(IcrsLog.event("ai.context-planner.decision",
                    "grievanceId", grievance.getId(),
                    "toolCount", usedTools.size(),
                    "tools", String.join(",", usedTools),
                    "plannerTrace", compact(plannerTrace)));
        }

        return new ContextCollectionResult(
                toolSet.policyContext(),
                toolSet.policyFetched(),
                toolSet.commentContext(),
                toolSet.commentFetched(),
                toolSet.statusHistoryContext(),
                toolSet.statusHistoryFetched(),
                toolSet.resolutionGuidanceContext(),
                toolSet.resolutionGuidanceFetched(),
                plannerTrace,
                routeTrace
        );
    }

    private String systemPrompt() {
        return """
                You are a context-planning agent for a college grievance workflow.
                Use the available tools only when they will materially improve later classification or resolution.
                Prefer the smallest useful context set.
                Do not call the same tool repeatedly.
                When you have enough context, stop calling tools and reply with a short plain-text note.
                """;
    }

    private String userPrompt(Grievance grievance, Sentiment sentiment, String ragContext) {
        return """
                Decide whether you need extra institutional context before downstream classification and resolution.

                Grievance:
                - title: %s
                - description: %s
                - category: %s
                - subcategory: %s
                - sentiment: %s

                Retrieved cases:
                %s
                """.formatted(
                safe(grievance.getTitle()),
                safe(truncate(grievance.getDescription(), icrsProperties.getAi().getMaxDescriptionChars())),
                grievance.getCategory() != null ? safe(grievance.getCategory().getName()) : "UNKNOWN",
                grievance.getSubcategory() != null ? safe(grievance.getSubcategory().getName()) : "UNKNOWN",
                sentiment != null ? sentiment.name() : "UNKNOWN",
                StringUtils.hasText(ragContext) ? ragContext : "No similar cases retrieved."
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

    private String routeTraceSegment(List<dev.langchain4j.agent.tool.ToolExecutionRequest> requests) {
        return requests.stream()
                .map(dev.langchain4j.agent.tool.ToolExecutionRequest::name)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private List<ToolSpecification> availableSpecifications(LC4jToolService toolService, Set<String> usedTools) {
        return toolService.toolSpecifications().stream()
                .filter(specification -> !usedTools.contains(specification.name()))
                .toList();
    }

    private List<ToolExecutionResultMessage> executeTools(LC4jToolService toolService, List<ToolExecutionRequest> requests) {
        InvocationContext context = InvocationContext.builder()
                .invocationId(UUID.randomUUID())
                .interfaceName(GrievanceContextPlannerAgent.class.getName())
                .methodName("collectContext")
                .methodArguments(List.of())
                .invocationParameters(InvocationParameters.from(Map.of()))
                .timestampNow()
                .build();

        var command = toolService.execute(requests, context, "messages").join();
        Object update = command.update().get("messages");
        if (update instanceof List<?> list) {
            return list.stream()
                    .filter(ToolExecutionResultMessage.class::isInstance)
                    .map(ToolExecutionResultMessage.class::cast)
                    .toList();
        }
        return List.of();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || maxLength <= 0) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    static final class ContextPlannerToolSet {

        private final Long grievanceId;
        private final GrievanceAgentContextService contextService;

        private String policyContext = "";
        private String commentContext = "";
        private String statusHistoryContext = "";
        private String resolutionGuidanceContext = "";
        private boolean policyFetched;
        private boolean commentFetched;
        private boolean statusHistoryFetched;
        private boolean resolutionGuidanceFetched;

        private ContextPlannerToolSet(Long grievanceId, GrievanceAgentContextService contextService) {
            this.grievanceId = grievanceId;
            this.contextService = contextService;
        }

        @Tool("Fetch policy and privacy context for the current grievance. Use when sensitivity, assignment, or institutional guardrails may affect the later decision.")
        public String loadPolicyContext() {
            policyContext = contextService.buildPolicyContext(grievanceId);
            policyFetched = true;
            return policyContext;
        }

        @Tool("Fetch the recent grievance comment thread. Use only when the discussion history may materially change classification or resolution.")
        public String loadCommentContext() {
            commentContext = contextService.buildCommentContext(grievanceId);
            commentFetched = true;
            return commentContext;
        }

        @Tool("Fetch recent grievance status-history transitions. Use when prior workflow changes may affect the current resolution path.")
        public String loadStatusHistoryContext() {
            statusHistoryContext = contextService.buildStatusHistoryContext(grievanceId);
            statusHistoryFetched = true;
            return statusHistoryContext;
        }

        @Tool("Fetch office, desk, building, room, contact, and open-hours guidance for the grievance category. Use when concrete student-routing guidance would improve the final resolution comment.")
        public String loadResolutionGuidanceContext() {
            resolutionGuidanceContext = contextService.buildResolutionGuidanceContext(grievanceId);
            resolutionGuidanceFetched = true;
            return resolutionGuidanceContext;
        }

        private String policyContext() {
            return policyContext;
        }

        private boolean policyFetched() {
            return policyFetched;
        }

        private String commentContext() {
            return commentContext;
        }

        private boolean commentFetched() {
            return commentFetched;
        }

        private String statusHistoryContext() {
            return statusHistoryContext;
        }

        private boolean statusHistoryFetched() {
            return statusHistoryFetched;
        }

        private String resolutionGuidanceContext() {
            return resolutionGuidanceContext;
        }

        private boolean resolutionGuidanceFetched() {
            return resolutionGuidanceFetched;
        }
    }
}
