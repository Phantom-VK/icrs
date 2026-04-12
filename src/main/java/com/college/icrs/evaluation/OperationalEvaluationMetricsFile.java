package com.college.icrs.evaluation;

public record OperationalEvaluationMetricsFile(
        OperationalEvaluationRunSummary summary,
        OperationalEvaluationMetrics metrics
) {
}
