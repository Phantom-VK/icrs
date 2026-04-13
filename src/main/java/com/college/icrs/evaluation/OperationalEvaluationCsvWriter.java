package com.college.icrs.evaluation;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class OperationalEvaluationCsvWriter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public void write(Path file, List<OperationalEvaluationResult> results) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        StringBuilder builder = new StringBuilder();
        builder.append(String.join(",",
                "experimentVariant",
                "caseId",
                "grievanceId",
                "experimentState",
                "failureReason",
                "title",
                "category",
                "subcategory",
                "sensitiveCategory",
                "submissionTimestamp",
                "aiDecisionTimestamp",
                "aiLatencyMillis",
                "finalStatus",
                "assignedFaculty",
                "priority",
                "sentiment",
                "aiResolved",
                "aiConfidence",
                "aiTitle",
                "aiModelName",
                "retrievedReferenceCount",
                "retrievedReferenceIds",
                "retrievedReferenceSources",
                "top1SimilarityScore",
                "averageRetrievedSimilarityScore"
        )).append('\n');

        for (OperationalEvaluationResult result : results) {
            List<OperationalEvaluationRetrievedReference> references = result.retrievedReferences();
            builder.append(String.join(",",
                    csv(result.experimentVariant()),
                    csv(result.caseId()),
                    csv(result.grievanceId()),
                    csv(result.experimentState()),
                    csv(result.failureReason()),
                    csv(result.title()),
                    csv(result.category()),
                    csv(result.subcategory()),
                    csv(result.sensitiveCategory()),
                    csv(result.submissionTimestamp()),
                    csv(result.aiDecisionTimestamp()),
                    csv(result.aiLatencyMillis()),
                    csv(result.finalStatus()),
                    csv(result.assignedFaculty()),
                    csv(result.priority()),
                    csv(result.sentiment()),
                    csv(result.aiResolved()),
                    csv(result.aiConfidence()),
                    csv(result.aiTitle()),
                    csv(result.aiModelName()),
                    csv(references.size()),
                    csv(join(references.stream().map(OperationalEvaluationRetrievedReference::referenceId).toList())),
                    csv(join(references.stream().map(OperationalEvaluationRetrievedReference::source).toList())),
                    csv(references.isEmpty() ? null : references.getFirst().similarityScore()),
                    csv(averageSimilarity(references))
            )).append('\n');
        }

        Files.writeString(file, builder.toString());
    }

    private String join(List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining("|"));
    }

    private Double averageSimilarity(List<OperationalEvaluationRetrievedReference> references) {
        return references.stream()
                .map(OperationalEvaluationRetrievedReference::similarityScore)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.NaN);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double doubleValue && Double.isNaN(doubleValue)) {
            return "";
        }
        String text = switch (value) {
            case java.time.LocalDateTime dateTime -> DATE_TIME_FORMATTER.format(dateTime);
            case Double doubleValue -> String.format(Locale.ROOT, "%.6f", doubleValue);
            default -> String.valueOf(value);
        };
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
