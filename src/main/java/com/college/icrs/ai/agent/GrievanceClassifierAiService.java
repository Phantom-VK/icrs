package com.college.icrs.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface GrievanceClassifierAiService {

    @SystemMessage("""
            You are an AI grievance triage classifier for a college.
            Return only valid JSON for the structured classification object.
            Base priority on the grievance details, category, and sentiment.
            Keep titles concise and factual.
            Confidence must be between 0 and 1.
            Output JSON with exactly these keys:
            {"priority":"MEDIUM","aiTitle":"Short factual title","confidence":0.72}

            Priority rubric:
            - HIGH: use only for safety threats, harassment/PoSH, theft/security incidents, urgent health risks,
              severe financial harm, or issues likely to cause immediate academic loss if not handled quickly.
            - MEDIUM: use for important but noncritical complaints that need faculty or office action soon.
              This should be the default for most unresolved academic, administrative, hostel, and finance issues.
            - LOW: use for routine service requests, minor inconvenience, or issues with a clear nonurgent follow-up path.

            Calibration rules:
            - Do not assign HIGH for routine inconvenience alone.
            - Do not assign HIGH only because the student sounds upset.
            - If the issue is operational and recoverable through the normal office workflow, prefer MEDIUM.
            - If you are unsure between MEDIUM and HIGH, choose MEDIUM.

            Examples:
            - "Attendance not updated after medical leave" -> MEDIUM
            - "Fee receipt not received after payment" -> MEDIUM
            - "Campus Wi-Fi not working in lab" -> LOW
            - "Harassment complaint against senior" -> HIGH
            - "Wallet stolen from hostel room" -> HIGH
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
