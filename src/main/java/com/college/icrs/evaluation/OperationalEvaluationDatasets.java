package com.college.icrs.evaluation;

import java.util.List;

public record OperationalEvaluationDatasets(
        List<OperationalEvaluationHistoricalCase> historicalCases,
        List<OperationalEvaluationLiveCase> liveCases
) {
}
