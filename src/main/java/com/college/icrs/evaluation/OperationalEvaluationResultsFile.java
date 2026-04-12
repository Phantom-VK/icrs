package com.college.icrs.evaluation;

import java.util.List;

public record OperationalEvaluationResultsFile(
        OperationalEvaluationRunSummary summary,
        List<OperationalEvaluationResult> results
) {
}
