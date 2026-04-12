package com.college.icrs.evaluation;

public record OperationalEvaluationRetrievedReference(
        String referenceId,
        String source,
        String category,
        String subcategory,
        Double similarityScore,
        boolean categoryMatchesInput
) {
}
