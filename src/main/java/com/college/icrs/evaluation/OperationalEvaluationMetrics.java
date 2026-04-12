package com.college.icrs.evaluation;

import java.util.Map;

public record OperationalEvaluationMetrics(
        int totalCases,
        long submittedCases,
        double submissionSuccessRate,
        long aiCompletedCases,
        double aiCompletionRate,
        long sentimentCompletedCases,
        double sentimentCompletionRate,
        long autoResolvedCases,
        double autoResolutionRate,
        ConfidenceDistribution confidenceDistribution,
        LatencyDistribution latencyDistribution,
        Map<String, Long> experimentStateDistribution,
        Map<String, Long> finalStatusDistribution,
        Map<String, CategoryMetrics> categoryAutoResolution,
        SensitiveHandlingMetrics sensitiveHandling,
        RetrievalMetrics retrievalMetrics
) {

    public record ConfidenceDistribution(
            Double mean,
            Double median,
            Double min,
            Double p10,
            Double p90,
            Double max
    ) {
    }

    public record LatencyDistribution(
            Double meanMillis,
            Double medianMillis
    ) {
    }

    public record CategoryMetrics(
            long totalCases,
            long autoResolvedCases,
            double autoResolutionRate
    ) {
    }

    public record SensitiveHandlingMetrics(
            long sensitiveCases,
            long sensitiveAutoResolvedCases,
            double sensitiveAutoResolutionRate,
            long nonSensitiveCases,
            long nonSensitiveAutoResolvedCases,
            double nonSensitiveAutoResolutionRate
    ) {
    }

    public record RetrievalMetrics(
            double averageRetrievedReferencesPerCase,
            double shareWithAtLeastOneReference,
            Double averageTop1SimilarityScore,
            Double averageRetrievedSimilarityScore,
            Double categoryMatchRate,
            Map<String, Long> sourceDistribution
    ) {
    }
}
