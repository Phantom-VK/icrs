package com.college.icrs.evaluation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

class OperationalEvaluationMetricsCalculatorTest {

    private final OperationalEvaluationMetricsCalculator calculator = new OperationalEvaluationMetricsCalculator();

    @Test
    void shouldComputeOperationalMetricsFromResultRows() {
        List<OperationalEvaluationResult> results = List.of(
                new OperationalEvaluationResult(
                        "rag_enabled",
                        "live-1",
                        "WiFi disconnects",
                        "IT Support",
                        "WiFi / Network",
                        false,
                        101L,
                        "completed",
                        null,
                        LocalDateTime.parse("2026-04-10T10:00:00"),
                        LocalDateTime.parse("2026-04-10T10:00:01"),
                        1000L,
                        "RESOLVED",
                        "it.support@college.edu",
                        "LOW",
                        "NEGATIVE",
                        true,
                        0.90d,
                        "WiFi issue",
                        "Reset the profile and reconnect.",
                        "deepseek-chat",
                        List.of(
                                new OperationalEvaluationRetrievedReference("hist-1", "manual-import", "IT Support", "WiFi / Network", 0.91d, true),
                                new OperationalEvaluationRetrievedReference("55", "application-grievance", "Finance & Scholarships", "Fee Payment", 0.71d, false)
                        )
                ),
                new OperationalEvaluationResult(
                        "rag_enabled",
                        "live-2",
                        "Sensitive complaint",
                        "Harassment / PoSH",
                        "PoSH Complaint",
                        true,
                        102L,
                        "timed_out",
                        "Timed out waiting for AI decision.",
                        LocalDateTime.parse("2026-04-10T10:05:00"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        null,
                        List.of()
                ),
                new OperationalEvaluationResult(
                        "rag_enabled",
                        "live-3",
                        "Fee receipt missing",
                        "Finance & Scholarships",
                        "Fee Payment",
                        false,
                        103L,
                        "completed",
                        null,
                        LocalDateTime.parse("2026-04-10T10:10:00"),
                        LocalDateTime.parse("2026-04-10T10:10:03"),
                        3000L,
                        "IN_PROGRESS",
                        "finance@college.edu",
                        "MEDIUM",
                        "NEGATIVE",
                        false,
                        0.50d,
                        "Receipt issue",
                        null,
                        "deepseek-chat",
                        List.of(
                                new OperationalEvaluationRetrievedReference("56", "application-grievance", "Finance & Scholarships", "Fee Payment", 0.63d, true)
                        )
                )
        );

        OperationalEvaluationMetrics metrics = calculator.calculate(results);

        assertThat(metrics.totalCases()).isEqualTo(3);
        assertThat(metrics.submittedCases()).isEqualTo(3);
        assertThat(metrics.submissionSuccessRate()).isEqualTo(1.0d, offset(0.0001d));
        assertThat(metrics.aiCompletedCases()).isEqualTo(2);
        assertThat(metrics.aiCompletionRate()).isEqualTo(2.0d / 3.0d, offset(0.0001d));
        assertThat(metrics.sentimentCompletedCases()).isEqualTo(2);
        assertThat(metrics.autoResolvedCases()).isEqualTo(1);
        assertThat(metrics.autoResolutionRate()).isEqualTo(1.0d / 3.0d, offset(0.0001d));

        assertThat(metrics.experimentStateDistribution()).containsEntry("completed", 2L);
        assertThat(metrics.experimentStateDistribution()).containsEntry("timed_out", 1L);
        assertThat(metrics.finalStatusDistribution()).containsEntry("RESOLVED", 1L);
        assertThat(metrics.finalStatusDistribution()).containsEntry("IN_PROGRESS", 1L);
        assertThat(metrics.finalStatusDistribution()).containsEntry("UNKNOWN", 1L);

        assertThat(metrics.categoryAutoResolution().get("IT Support").autoResolutionRate()).isEqualTo(1.0d, offset(0.0001d));
        assertThat(metrics.categoryAutoResolution().get("Harassment / PoSH").autoResolutionRate()).isZero();

        assertThat(metrics.sensitiveHandling().sensitiveCases()).isEqualTo(1);
        assertThat(metrics.sensitiveHandling().sensitiveAutoResolvedCases()).isZero();
        assertThat(metrics.sensitiveHandling().nonSensitiveCases()).isEqualTo(2);
        assertThat(metrics.sensitiveHandling().nonSensitiveAutoResolvedCases()).isEqualTo(1);

        assertThat(metrics.retrievalMetrics().averageRetrievedReferencesPerCase()).isEqualTo(1.0d, offset(0.0001d));
        assertThat(metrics.retrievalMetrics().shareWithAtLeastOneReference()).isEqualTo(2.0d / 3.0d, offset(0.0001d));
        assertThat(metrics.retrievalMetrics().averageTop1SimilarityScore()).isEqualTo(0.77d, offset(0.0001d));
        assertThat(metrics.retrievalMetrics().averageRetrievedSimilarityScore()).isEqualTo(0.75d, offset(0.0001d));
        assertThat(metrics.retrievalMetrics().categoryMatchRate()).isEqualTo(2.0d / 3.0d, offset(0.0001d));
        assertThat(metrics.retrievalMetrics().sourceDistribution()).containsEntry("manual-import", 1L);
        assertThat(metrics.retrievalMetrics().sourceDistribution()).containsEntry("application-grievance", 2L);

        assertThat(metrics.confidenceDistribution().mean()).isEqualTo(0.70d, offset(0.0001d));
        assertThat(metrics.latencyDistribution().meanMillis()).isEqualTo(2000.0d, offset(0.0001d));
        assertThat(metrics.latencyDistribution().medianMillis()).isEqualTo(1000.0d, offset(0.0001d));
    }
}
