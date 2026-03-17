package com.college.icrs.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface GrievanceClassifierAiService {

    @SystemMessage("""
            You are an AI grievance triage classifier for a college.
            Return a structured classification object.
            Base priority on the grievance details, category, and sentiment.
            Keep titles concise and factual.
            Confidence must be between 0 and 1.
            """)
    @UserMessage("""
            Classify this grievance.

            Context:
            - title: {{title}}
            - description: {{description}}
            - category: {{category}}
            - subcategory: {{subcategory}}
            - sentiment: {{sentiment}}

            Retrieved cases:
            {{ragContext}}

            Policy signals:
            {{policyContext}}

            Comment thread:
            {{commentContext}}

            Status history:
            {{statusHistoryContext}}
            """)
    ClassificationDecision classify(
            @V("title") String title,
            @V("description") String description,
            @V("category") String category,
            @V("subcategory") String subcategory,
            @V("sentiment") String sentiment,
            @V("ragContext") String ragContext,
            @V("policyContext") String policyContext,
            @V("commentContext") String commentContext,
            @V("statusHistoryContext") String statusHistoryContext
    );
}
