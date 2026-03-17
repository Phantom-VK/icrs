package com.college.icrs.ai.agent;

import com.college.icrs.ai.knowledge.ResolutionGuidanceService;
import com.college.icrs.ai.service.SentimentAnalysisService;
import com.college.icrs.model.Category;
import com.college.icrs.model.Comment;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.StatusHistory;
import com.college.icrs.rag.RagService;
import com.college.icrs.repository.CommentRepository;
import com.college.icrs.repository.StatusHistoryRepository;
import com.college.icrs.service.GrievanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GrievanceAgentContextService {

    private final GrievanceService grievanceService;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final RagService ragService;
    private final CommentRepository commentRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final ResolutionGuidanceService resolutionGuidanceService;

    public Grievance loadGrievance(Long grievanceId) {
        return grievanceService.getGrievanceById(grievanceId);
    }

    public SentimentAnalysisService.SentimentDecision analyzeSentiment(Grievance grievance) {
        return sentimentAnalysisService.analyze(grievance != null ? grievance.getDescription() : null);
    }

    public List<RagService.GrievanceContext> retrieveSimilar(Grievance grievance) {
        return ragService.retrieveSimilar(grievance);
    }

    public String buildContextSection(List<RagService.GrievanceContext> contexts) {
        return ragService.buildContextSection(contexts);
    }

    public String buildPolicyContext(Long grievanceId) {
        Grievance grievance = grievanceService.getGrievanceById(grievanceId);
        Category category = grievance.getCategory();
        return """
                - currentStatus: %s
                - sensitiveCategory: %s
                - hideIdentity: %s
                - assignedTo: %s
                """.formatted(
                grievance.getStatus(),
                category != null && Boolean.TRUE.equals(category.getSensitive()),
                category != null && Boolean.TRUE.equals(category.getHideIdentity()),
                grievance.getAssignedTo() != null ? safe(grievance.getAssignedTo().getEmail()) : "UNASSIGNED"
        ).trim();
    }

    public String buildCommentContext(Long grievanceId) {
        List<Comment> comments = commentRepository.findByGrievanceIdOrderByCreatedAtAsc(grievanceId);
        if (comments.isEmpty()) {
            return "No comments yet.";
        }
        return comments.stream()
                .skip(Math.max(0, comments.size() - 5L))
                .map(comment -> "- %s: %s".formatted(
                        comment.getAuthor() != null ? safe(comment.getAuthor().getUsername()) : "Unknown",
                        truncate(safe(comment.getBody()).replaceAll("\\s+", " "), 240)
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No comments yet.");
    }

    public String buildStatusHistoryContext(Long grievanceId) {
        List<StatusHistory> history = statusHistoryRepository.findByGrievanceIdOrderByChangedAtDesc(grievanceId);
        if (history.isEmpty()) {
            return "No prior status transitions.";
        }
        return history.stream()
                .limit(5)
                .map(entry -> "- %s -> %s (%s)".formatted(
                        entry.getFromStatus(),
                        entry.getToStatus(),
                        StringUtils.hasText(entry.getReason()) ? truncate(entry.getReason().replaceAll("\\s+", " "), 180) : "no reason"
                ))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No prior status transitions.");
    }

    public String buildResolutionGuidanceContext(Long grievanceId) {
        return resolutionGuidanceService.buildContext(grievanceService.getGrievanceById(grievanceId));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || maxLength <= 0) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
