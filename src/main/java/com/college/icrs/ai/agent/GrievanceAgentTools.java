package com.college.icrs.ai.agent;

import com.college.icrs.ai.service.SentimentAnalysisService;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Sentiment;
import com.college.icrs.rag.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GrievanceAgentTools {

    private final GrievanceAgentContextService contextService;
    private final GrievanceAgentDecisionService decisionService;
    private final GrievanceAgentActionService actionService;

    public Grievance loadGrievance(Long grievanceId) {
        return contextService.loadGrievance(grievanceId);
    }

    public SentimentAnalysisService.SentimentDecision analyzeSentiment(Grievance grievance) {
        return contextService.analyzeSentiment(grievance);
    }

    public List<RagService.GrievanceContext> retrieveSimilar(Grievance grievance) {
        return contextService.retrieveSimilar(grievance);
    }

    public String buildContextSection(List<RagService.GrievanceContext> contexts) {
        return contextService.buildContextSection(contexts);
    }

    public String buildPolicyContext(Long grievanceId) {
        return contextService.buildPolicyContext(grievanceId);
    }

    public String buildCommentContext(Long grievanceId) {
        return contextService.buildCommentContext(grievanceId);
    }

    public String buildStatusHistoryContext(Long grievanceId) {
        return contextService.buildStatusHistoryContext(grievanceId);
    }

    public String buildResolutionGuidanceContext(Long grievanceId) {
        return contextService.buildResolutionGuidanceContext(grievanceId);
    }

    public ContextToolSelection selectContextTools(
            Grievance grievance,
            Sentiment sentiment,
            String ragContext,
            String policyContext,
            String commentContext,
            String statusHistoryContext,
            String resolutionGuidanceContext,
            boolean policyFetched,
            boolean commentFetched,
            boolean statusHistoryFetched,
            boolean resolutionGuidanceFetched,
            int plannerIteration
    ) {
        return decisionService.selectContextTools(
                grievance,
                sentiment,
                ragContext,
                policyContext,
                commentContext,
                statusHistoryContext,
                resolutionGuidanceContext,
                policyFetched,
                commentFetched,
                statusHistoryFetched,
                resolutionGuidanceFetched,
                plannerIteration
        );
    }

    public ClassificationDecision classify(
            Grievance grievance,
            Sentiment sentiment,
            String ragContext,
            String policyContext,
            String commentContext,
            String statusHistoryContext
    ) throws Exception {
        return decisionService.classify(grievance, sentiment, ragContext, policyContext, commentContext, statusHistoryContext);
    }

    public void applyClassificationMetadata(
            Long grievanceId,
            Sentiment sentiment,
            String sentimentModelName,
            String priorityValue,
            String aiTitleValue,
            Double confidenceValue
    ) {
        actionService.applyClassificationMetadata(
                grievanceId,
                sentiment,
                sentimentModelName,
                priorityValue,
                aiTitleValue,
                confidenceValue
        );
    }

    public ResolutionDecision resolve(
            Grievance grievance,
            Sentiment sentiment,
            String ragContext,
            String policyContext,
            String commentContext,
            String statusHistoryContext,
            String resolutionGuidanceContext
    ) throws Exception {
        return decisionService.resolve(
                grievance,
                sentiment,
                ragContext,
                policyContext,
                commentContext,
                statusHistoryContext,
                resolutionGuidanceContext
        );
    }

    public void finalizeDecision(
            Long grievanceId,
            String sentimentModelName,
            Double classificationConfidenceValue,
            Boolean resolutionAutoResolve,
            String resolutionTextValue,
            String resolutionInternalCommentValue,
            Double resolutionConfidenceValue
    ) {
        actionService.finalizeDecision(
                grievanceId,
                sentimentModelName,
                classificationConfidenceValue,
                resolutionAutoResolve,
                resolutionTextValue,
                resolutionInternalCommentValue,
                resolutionConfidenceValue
        );
    }
}
