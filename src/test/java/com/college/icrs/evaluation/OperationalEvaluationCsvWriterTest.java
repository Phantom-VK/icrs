package com.college.icrs.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalEvaluationCsvWriterTest {

    private final OperationalEvaluationCsvWriter writer = new OperationalEvaluationCsvWriter();

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteExperimentVariantIntoCsvHeaderAndRows() throws Exception {
        Path csvFile = tempDir.resolve("results.csv");

        writer.write(csvFile, List.of(
                new OperationalEvaluationResult(
                        "rag_disabled",
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
                                new OperationalEvaluationRetrievedReference("hist-1", "manual-import", "IT Support", "WiFi / Network", 0.91d, true)
                        )
                )
        ));

        List<String> lines = Files.readAllLines(csvFile);

        assertThat(lines).hasSize(2);
        assertThat(lines.getFirst()).startsWith("experimentVariant,caseId,grievanceId");
        assertThat(lines.get(1)).startsWith("\"rag_disabled\",\"live-1\",\"101\"");
    }
}
