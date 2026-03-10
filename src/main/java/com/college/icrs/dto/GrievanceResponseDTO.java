package com.college.icrs.dto;

import com.college.icrs.model.Priority;
import com.college.icrs.model.Sentiment;
import com.college.icrs.model.Status;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class GrievanceResponseDTO {

    private long id;
    private String title;
    private  String description;
    private Long categoryId;
    private String categoryName;
    private String category;
    private Long subcategoryId;
    private String subcategoryName;
    private String subcategory;
    private String registrationNumber;
    private String maskedRegistrationNumber;
    private Status status;
    private Priority priority;
    private Sentiment sentiment;
    private boolean aiResolved;
    private Double aiConfidence;
    private String aiTitle;
    private String aiResolutionText;
    private String aiResolutionComment;
    private String aiModelName;
    private LocalDateTime aiDecisionAt;
    private String aiDecisionSource;
    private String studentName;
    private boolean identityHidden;
    private boolean sensitiveCategory;
    private String assignedToName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StatusHistoryItem> statusHistory;

    @Getter
    @Setter
    public static class StatusHistoryItem {
        private String fromStatus;
        private String toStatus;
        private String actorName;
        private LocalDateTime changedAt;
        private String reason;
    }

}
