package com.college.icrs.rag;

import com.college.icrs.model.Comment;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import com.college.icrs.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResolvedGrievanceCommentSummaryService {

    private static final int MAX_COMMENT_ENTRIES = 4;
    private static final int MAX_ENTRY_LENGTH = 120;
    private static final int MAX_SUMMARY_LENGTH = 320;

    private final CommentRepository commentRepository;

    public String summarizeForEmbedding(Grievance grievance) {
        if (grievance == null || grievance.getId() <= 0L || grievance.getStatus() != Status.RESOLVED) {
            return null;
        }

        List<String> entries = commentRepository.findByGrievanceIdOrderByCreatedAtAsc(grievance.getId()).stream()
                .map(this::formatEntry)
                .filter(StringUtils::hasText)
                .toList();

        if (entries.isEmpty()) {
            return null;
        }

        int fromIndex = Math.max(0, entries.size() - MAX_COMMENT_ENTRIES);
        String summary = String.join(" | ", entries.subList(fromIndex, entries.size()));
        return summary.length() <= MAX_SUMMARY_LENGTH
                ? summary
                : summary.substring(0, MAX_SUMMARY_LENGTH);
    }

    private String formatEntry(Comment comment) {
        if (comment == null || !StringUtils.hasText(comment.getBody())) {
            return null;
        }

        String author = comment.getAuthor() != null && StringUtils.hasText(comment.getAuthor().getDisplayName())
                ? comment.getAuthor().getDisplayName().trim()
                : comment.getAuthor() != null && StringUtils.hasText(comment.getAuthor().getEmail())
                ? comment.getAuthor().getEmail().trim()
                : "Unknown";

        String body = comment.getBody().trim().replaceAll("\\s+", " ");
        if (body.length() > MAX_ENTRY_LENGTH) {
            body = body.substring(0, MAX_ENTRY_LENGTH);
        }
        return author + ": " + body;
    }
}
