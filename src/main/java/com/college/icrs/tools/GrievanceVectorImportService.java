package com.college.icrs.tools;

import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Priority;
import com.college.icrs.model.Sentiment;
import com.college.icrs.rag.GrievanceVectorDocumentFactory;
import com.college.icrs.rag.GrievanceVectorDocumentFactory.ImportedGrievanceRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceVectorImportService {

    private final ObjectMapper objectMapper;
    private final VectorStore vectorStore;
    private final GrievanceVectorDocumentFactory documentFactory;

    public void importFile(Path file, boolean replaceExisting) {
        if (file == null || !Files.exists(file) || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Import file not found: " + file);
        }

        List<Document> documents = parseDocuments(file);
        if (documents.isEmpty()) {
            log.warn(IcrsLog.event("rag.import.skipped", "reason", "no-documents", "file", file.toAbsolutePath()));
            return;
        }

        if (replaceExisting) {
            vectorStore.delete(documents.stream().map(Document::getId).toList());
        }

        vectorStore.add(documents);
        log.info(IcrsLog.event(
                "rag.import.completed",
                "file", file.toAbsolutePath(),
                "documents", documents.size(),
                "replaceExisting", replaceExisting
        ));
    }

    private List<Document> parseDocuments(Path file) {
        try {
            JsonNode root = objectMapper.readTree(file.toFile());
            JsonNode grievances = root.isArray() ? root : root.path("grievances");
            if (!grievances.isArray()) {
                throw new IllegalArgumentException("Expected a JSON array or an object with a grievances array");
            }

            List<Document> documents = new ArrayList<>();
            int index = 0;
            for (JsonNode node : grievances) {
                ImportedGrievanceRecord record = toRecord(node, index++);
                documents.add(documentFactory.fromImportedRecord(record));
            }
            return documents;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read import file: " + file, e);
        }
    }

    private ImportedGrievanceRecord toRecord(JsonNode node, int index) {
        String title = requiredText(node, "title");
        String description = requiredText(node, "description");

        Long grievanceId = null;
        if (node.hasNonNull("grievanceId")) {
            grievanceId = node.get("grievanceId").asLong();
        } else if (node.hasNonNull("id") && node.get("id").canConvertToLong()) {
            grievanceId = node.get("id").asLong();
        }

        String documentId = firstNonBlank(
                text(node, "documentId"),
                grievanceId != null ? String.valueOf(grievanceId) : null,
                text(node, "id"),
                "manual-" + (index + 1)
        );

        return new ImportedGrievanceRecord(
                documentId,
                grievanceId,
                title,
                description,
                text(node, "category"),
                text(node, "subcategory"),
                text(node, "registrationNumber"),
                parsePriority(text(node, "priority")),
                parseSentiment(text(node, "sentiment")),
                firstNonBlank(text(node, "resolutionText"), text(node, "aiResolutionText")),
                firstNonBlank(text(node, "commentSummary"), commentSummary(node.get("comments")))
        );
    }

    private String requiredText(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
        return value;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private String commentSummary(JsonNode commentsNode) {
        if (commentsNode == null || commentsNode.isNull()) {
            return null;
        }
        if (commentsNode.isTextual()) {
            return textValue(commentsNode.asText());
        }
        if (!commentsNode.isArray()) {
            return null;
        }

        List<String> entries = new ArrayList<>();
        for (JsonNode commentNode : commentsNode) {
            String entry = commentEntry(commentNode);
            if (StringUtils.hasText(entry)) {
                entries.add(entry);
            }
        }
        return entries.isEmpty() ? null : String.join(" | ", entries);
    }

    private String commentEntry(JsonNode commentNode) {
        if (commentNode == null || commentNode.isNull()) {
            return null;
        }
        if (commentNode.isTextual()) {
            return textValue(commentNode.asText());
        }

        String author = firstNonBlank(text(commentNode, "author"), text(commentNode, "authorName"), text(commentNode, "role"));
        String body = firstNonBlank(text(commentNode, "body"), text(commentNode, "comment"), text(commentNode, "text"));
        if (!StringUtils.hasText(author) && !StringUtils.hasText(body)) {
            return null;
        }
        if (!StringUtils.hasText(author)) {
            return body;
        }
        if (!StringUtils.hasText(body)) {
            return author;
        }
        return author + ": " + body;
    }

    private String textValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Priority parsePriority(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Sentiment parseSentiment(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Sentiment.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
