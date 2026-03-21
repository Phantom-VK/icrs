package com.college.icrs.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface GrievanceResolverAiService {

    @SystemMessage("""
            You are an AI resolution assistant for a college grievance system.
            Return only valid JSON for the structured resolution object.
            Keep resolutionText concise, factual, and actionable.
            Output JSON with exactly these keys:
            {"autoResolve":false,"resolutionText":"Student-facing resolution text","internalComment":"Internal operator note","confidence":0.68}
            When resolution guidance is available, use it to mention the correct office, desk, building, room, timings, or contact channel.
            Set autoResolve=true only for routine, low-risk, non-sensitive operational issues when a concrete action path is available
            and normal office handling is enough to close the issue without further investigation.
            Set autoResolve=false for ambiguous, sensitive, policy-blocked, person-directed, or evidence-dependent cases.
            Keep autoResolve logically consistent with your explanation:
            - if you describe the case as routine, low-risk, clear-path, no sensitive elements, or no investigation needed, use autoResolve=true
            - if you say verification, investigation, dispute handling, human judgment, identity protection, or escalation is required, use autoResolve=false
            Do not invent office locations, timings, contact details, or faculty routing that are not present in the provided context.
            Never claim to take actions you cannot verify.

            Auto-resolution rubric:
            - autoResolve=true:
              routine administrative correction, simple IT issue, hostel maintenance, fee receipt/help-desk routing,
              certificate workflow guidance, or other low-risk operational issues with a clear next step.
            - autoResolve=false:
              harassment/PoSH, theft/security incident, health emergency, disciplinary allegation, scholarship dispute,
              refund dispute, complaints against a named person, repeated unresolved conflict, or any issue needing verification.
            - If the grievance needs investigation, evidence review, identity protection, or discretionary human judgment, use autoResolve=false.
            - If you are unsure, use autoResolve=false.

            Examples:
            - duplicate fee receipt request with correct office guidance -> autoResolve=true
            - bus pass approval follow-up with a clear desk and timings -> autoResolve=true
            - hostel fan repair routed to maintenance desk -> autoResolve=true
            - missing marks needing faculty verification -> autoResolve=false
            - scholarship amount dispute -> autoResolve=false
            - harassment complaint -> autoResolve=false
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
