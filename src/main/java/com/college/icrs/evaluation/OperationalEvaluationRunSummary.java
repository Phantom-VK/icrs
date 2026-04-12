package com.college.icrs.evaluation;

import java.time.LocalDateTime;

public record OperationalEvaluationRunSummary(
        String backendBaseUrl,
        String historicalFile,
        String liveFile,
        long submissionPauseMs,
        long perCaseTimeoutMs,
        boolean aiEnabled,
        boolean sentimentEnabled,
        int historicalCasesValidated,
        int liveCasesValidated,
        boolean stoppedEarly,
        String stopReason,
        EnvironmentHealth environmentHealth,
        CleanupSummary cleanupSummary,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {

    public record EnvironmentHealth(
            boolean backendReachable,
            int backendCategoryCount,
            boolean databaseReachable,
            Integer vectorStoreCountBeforeRun,
            boolean sentimentServiceHealthy,
            String sentimentBaseUrl
    ) {
    }

    public record CleanupSummary(
            String studentEmail,
            int deletedExistingLiveGrievances,
            int deletedExistingLiveVectorDocuments,
            int clearedHistoricalVectorDocuments
    ) {
    }
}
