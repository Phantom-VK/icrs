package com.college.icrs.evaluation;

import java.time.LocalDateTime;
import java.util.List;

public record OperationalEvaluationResult(
        String experimentVariant,
        String caseId,
        String title,
        String category,
        String subcategory,
        boolean sensitiveCategory,
        Long grievanceId,
        String experimentState,
        String failureReason,
        LocalDateTime submissionTimestamp,
        LocalDateTime aiDecisionTimestamp,
        Long aiLatencyMillis,
        String finalStatus,
        String assignedFaculty,
        String priority,
        String sentiment,
        boolean aiResolved,
        Double aiConfidence,
        String aiTitle,
        String aiResolutionText,
        String aiModelName,
        List<OperationalEvaluationRetrievedReference> retrievedReferences
) {
}
