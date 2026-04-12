package com.college.icrs.evaluation;

import java.nio.file.Path;
import java.time.Duration;

public record OperationalEvaluationConfig(
        String backendBaseUrl,
        Path historicalFile,
        Path liveFile,
        Duration submissionPause,
        Duration perCaseTimeout,
        Path outputDir,
        String studentEmail,
        String studentPassword,
        String studentName,
        String studentDepartment,
        String studentId,
        int earlyFailureWindow,
        int earlyFailureThreshold
) {

    private static final String DEFAULT_BACKEND_BASE_URL = "http://localhost:8080";
    private static final Path DEFAULT_HISTORICAL_FILE = Path.of("evaluation/operational/historical-rag-smoke.json");
    private static final Path DEFAULT_LIVE_FILE = Path.of("evaluation/operational/live-grievances-smoke.json");
    private static final Path DEFAULT_OUTPUT_DIR = Path.of("build/reports/operational-evaluation");
    private static final Duration DEFAULT_SUBMISSION_PAUSE = Duration.ofSeconds(15);
    private static final Duration DEFAULT_PER_CASE_TIMEOUT = Duration.ofMinutes(3);

    public static OperationalEvaluationConfig fromSystemProperties() {
        return new OperationalEvaluationConfig(
                systemProperty("operationalEvaluationBackendBaseUrl", DEFAULT_BACKEND_BASE_URL),
                Path.of(systemProperty("operationalEvaluationHistoricalFile", DEFAULT_HISTORICAL_FILE.toString())),
                Path.of(systemProperty("operationalEvaluationLiveFile", DEFAULT_LIVE_FILE.toString())),
                Duration.ofMillis(longProperty("operationalEvaluationPauseMs", DEFAULT_SUBMISSION_PAUSE.toMillis())),
                Duration.ofMillis(longProperty("operationalEvaluationTimeoutMs", DEFAULT_PER_CASE_TIMEOUT.toMillis())),
                Path.of(systemProperty("operationalEvaluationOutputDir", DEFAULT_OUTPUT_DIR.toString())),
                systemProperty("operationalEvaluationStudentEmail", "evaluation.student@icrs.local"),
                systemProperty("operationalEvaluationStudentPassword", "Test@12345"),
                systemProperty("operationalEvaluationStudentName", "Operational Evaluation Student"),
                systemProperty("operationalEvaluationStudentDepartment", "IT"),
                systemProperty("operationalEvaluationStudentId", "EVAL-STUDENT-001"),
                intProperty("operationalEvaluationEarlyFailureWindow", 5),
                intProperty("operationalEvaluationEarlyFailureThreshold", 3)
        );
    }

    private static String systemProperty(String key, String fallback) {
        String value = System.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static long longProperty(String key, long fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Long.parseLong(value.trim());
    }

    private static int intProperty(String key, int fallback) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.trim());
    }
}
