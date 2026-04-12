package com.college.icrs.evaluation;

public record OperationalEvaluationHistoricalCase(
        String documentId,
        String title,
        String description,
        String category,
        String subcategory,
        String registrationNumber,
        String priority,
        String sentiment,
        String resolutionText,
        boolean sensitiveCategory
) {
}
