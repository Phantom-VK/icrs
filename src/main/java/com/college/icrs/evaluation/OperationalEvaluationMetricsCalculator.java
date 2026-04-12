package com.college.icrs.evaluation;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class OperationalEvaluationMetricsCalculator {

    public OperationalEvaluationMetrics calculate(List<OperationalEvaluationResult> results) {
        List<OperationalEvaluationResult> safeResults = results != null ? results : List.of();
        int totalCases = safeResults.size();
        long submittedCases = safeResults.stream().filter(result -> result.grievanceId() != null).count();
        long aiCompletedCases = safeResults.stream()
                .filter(result -> "completed".equalsIgnoreCase(result.experimentState()))
                .count();
        long sentimentCompletedCases = safeResults.stream()
                .filter(result -> StringUtils.hasText(result.sentiment()))
                .count();
        long autoResolvedCases = safeResults.stream()
                .filter(OperationalEvaluationResult::aiResolved)
                .count();

        Map<String, Long> experimentStateDistribution = distribution(
                safeResults.stream().map(OperationalEvaluationResult::experimentState).toList()
        );
        Map<String, Long> finalStatusDistribution = distribution(
                safeResults.stream().map(OperationalEvaluationResult::finalStatus).toList()
        );

        Map<String, OperationalEvaluationMetrics.CategoryMetrics> categoryMetrics = categoryMetrics(safeResults);
        OperationalEvaluationMetrics.SensitiveHandlingMetrics sensitiveHandling = sensitiveHandling(safeResults);
        OperationalEvaluationMetrics.RetrievalMetrics retrievalMetrics = retrievalMetrics(safeResults);

        List<Double> confidences = safeResults.stream()
                .map(OperationalEvaluationResult::aiConfidence)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

        List<Double> latencies = safeResults.stream()
                .map(OperationalEvaluationResult::aiLatencyMillis)
                .filter(Objects::nonNull)
                .map(Long::doubleValue)
                .sorted()
                .toList();

        return new OperationalEvaluationMetrics(
                totalCases,
                submittedCases,
                rate(submittedCases, totalCases),
                aiCompletedCases,
                rate(aiCompletedCases, totalCases),
                sentimentCompletedCases,
                rate(sentimentCompletedCases, totalCases),
                autoResolvedCases,
                rate(autoResolvedCases, totalCases),
                new OperationalEvaluationMetrics.ConfidenceDistribution(
                        mean(confidences),
                        percentile(confidences, 0.50d),
                        confidences.isEmpty() ? null : confidences.getFirst(),
                        percentile(confidences, 0.10d),
                        percentile(confidences, 0.90d),
                        confidences.isEmpty() ? null : confidences.getLast()
                ),
                new OperationalEvaluationMetrics.LatencyDistribution(
                        mean(latencies),
                        percentile(latencies, 0.50d)
                ),
                experimentStateDistribution,
                finalStatusDistribution,
                categoryMetrics,
                sensitiveHandling,
                retrievalMetrics
        );
    }

    private Map<String, OperationalEvaluationMetrics.CategoryMetrics> categoryMetrics(List<OperationalEvaluationResult> results) {
        Map<String, List<OperationalEvaluationResult>> byCategory = new LinkedHashMap<>();
        for (OperationalEvaluationResult result : results) {
            String category = normalizeKey(result.category());
            byCategory.computeIfAbsent(category, ignored -> new ArrayList<>()).add(result);
        }

        Map<String, OperationalEvaluationMetrics.CategoryMetrics> metrics = new LinkedHashMap<>();
        byCategory.forEach((category, categoryResults) -> {
            long autoResolvedCases = categoryResults.stream().filter(OperationalEvaluationResult::aiResolved).count();
            metrics.put(category, new OperationalEvaluationMetrics.CategoryMetrics(
                    categoryResults.size(),
                    autoResolvedCases,
                    rate(autoResolvedCases, categoryResults.size())
            ));
        });
        return metrics;
    }

    private OperationalEvaluationMetrics.SensitiveHandlingMetrics sensitiveHandling(List<OperationalEvaluationResult> results) {
        List<OperationalEvaluationResult> sensitive = results.stream().filter(OperationalEvaluationResult::sensitiveCategory).toList();
        List<OperationalEvaluationResult> nonSensitive = results.stream().filter(result -> !result.sensitiveCategory()).toList();
        long sensitiveAutoResolved = sensitive.stream().filter(OperationalEvaluationResult::aiResolved).count();
        long nonSensitiveAutoResolved = nonSensitive.stream().filter(OperationalEvaluationResult::aiResolved).count();

        return new OperationalEvaluationMetrics.SensitiveHandlingMetrics(
                sensitive.size(),
                sensitiveAutoResolved,
                rate(sensitiveAutoResolved, sensitive.size()),
                nonSensitive.size(),
                nonSensitiveAutoResolved,
                rate(nonSensitiveAutoResolved, nonSensitive.size())
        );
    }

    private OperationalEvaluationMetrics.RetrievalMetrics retrievalMetrics(List<OperationalEvaluationResult> results) {
        int totalCases = results.size();
        List<OperationalEvaluationRetrievedReference> references = results.stream()
                .flatMap(result -> result.retrievedReferences().stream())
                .toList();

        long withAtLeastOneReference = results.stream()
                .filter(result -> !result.retrievedReferences().isEmpty())
                .count();

        List<Double> top1Scores = results.stream()
                .map(OperationalEvaluationResult::retrievedReferences)
                .filter(referenceList -> !referenceList.isEmpty())
                .map(referenceList -> referenceList.getFirst().similarityScore())
                .filter(Objects::nonNull)
                .toList();

        List<Double> allSimilarityScores = references.stream()
                .map(OperationalEvaluationRetrievedReference::similarityScore)
                .filter(Objects::nonNull)
                .toList();

        long categoryMatches = references.stream()
                .filter(OperationalEvaluationRetrievedReference::categoryMatchesInput)
                .count();

        Map<String, Long> sourceDistribution = distribution(
                references.stream().map(OperationalEvaluationRetrievedReference::source).toList()
        );

        double averageRetrievedReferencesPerCase = totalCases == 0
                ? 0.0d
                : references.size() / (double) totalCases;

        return new OperationalEvaluationMetrics.RetrievalMetrics(
                averageRetrievedReferencesPerCase,
                rate(withAtLeastOneReference, totalCases),
                mean(top1Scores),
                mean(allSimilarityScores),
                references.isEmpty() ? null : rate(categoryMatches, references.size()),
                sourceDistribution
        );
    }

    private Map<String, Long> distribution(List<String> values) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (String value : values) {
            String key = normalizeKey(value);
            distribution.put(key, distribution.getOrDefault(key, 0L) + 1L);
        }
        return distribution;
    }

    private String normalizeKey(String value) {
        return StringUtils.hasText(value) ? value.trim() : "UNKNOWN";
    }

    private Double mean(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private Double percentile(List<Double> values, double quantile) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<Double> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int index = (int) Math.ceil(quantile * sorted.size()) - 1;
        int safeIndex = Math.max(0, Math.min(sorted.size() - 1, index));
        return sorted.get(safeIndex);
    }

    private double rate(long numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0d;
        }
        return numerator / (double) denominator;
    }
}
