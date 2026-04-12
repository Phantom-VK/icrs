package com.college.icrs.evaluation;

import java.util.List;

public record OperationalEvaluationReport(
        OperationalEvaluationRunSummary summary,
        OperationalEvaluationMetrics metrics,
        List<OperationalEvaluationResult> results
) {
}
