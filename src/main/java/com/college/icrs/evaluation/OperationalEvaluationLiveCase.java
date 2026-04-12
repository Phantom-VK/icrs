package com.college.icrs.evaluation;

public record OperationalEvaluationLiveCase(
        String caseId,
        String title,
        String description,
        String category,
        String subcategory,
        String registrationNumber,
        boolean sensitiveCategory
) {
}
