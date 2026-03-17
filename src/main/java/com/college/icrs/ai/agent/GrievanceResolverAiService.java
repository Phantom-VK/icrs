package com.college.icrs.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface GrievanceResolverAiService {

    @SystemMessage("""
            You are an AI resolution assistant for a college grievance system.
            Return a structured resolution object.
            Keep resolutionText concise, factual, and actionable.
            When resolution guidance is available, use it to mention the correct office, desk, building, room, timings, or contact channel.
            Set autoResolve=true for routine, low-risk, non-sensitive operational issues when a concrete action path is available
            and  autoResolve=false for ambiguous, sensitive, or policy-blocked cases.
            Do not invent office locations, timings, contact details, or faculty routing that are not present in the provided context.
            Never claim to take actions you cannot verify.
            """)
    @UserMessage("""
            Generate a resolution decision for this grievance.

            Context:
            - title: {{title}}
            - description: {{description}}
            - category: {{category}}
            - subcategory: {{subcategory}}
            - sentiment: {{sentiment}}
            - classificationTitle: {{classificationTitle}}

            Retrieved cases:
            {{ragContext}}

            Policy signals:
            {{policyContext}}

            Comment thread:
            {{commentContext}}

            Status history:
            {{statusHistoryContext}}

            Resolution guidance:
            {{resolutionGuidanceContext}}
            """)
    ResolutionDecision resolve(
            @V("title") String title,
            @V("description") String description,
            @V("category") String category,
            @V("subcategory") String subcategory,
            @V("sentiment") String sentiment,
            @V("classificationTitle") String classificationTitle,
            @V("ragContext") String ragContext,
            @V("policyContext") String policyContext,
            @V("commentContext") String commentContext,
            @V("statusHistoryContext") String statusHistoryContext,
            @V("resolutionGuidanceContext") String resolutionGuidanceContext
    );
}
