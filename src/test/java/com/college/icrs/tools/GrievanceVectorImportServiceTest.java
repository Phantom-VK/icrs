package com.college.icrs.tools;

import com.college.icrs.rag.GrievanceVectorDocumentFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GrievanceVectorImportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void importsCommentThreadIntoVectorDocument() throws Exception {
        VectorStore vectorStore = mock(VectorStore.class);
        GrievanceVectorImportService service = new GrievanceVectorImportService(
                new ObjectMapper(),
                vectorStore,
                new GrievanceVectorDocumentFactory()
        );

        Path file = tempDir.resolve("historical.json");
        java.nio.file.Files.writeString(file, """
                [
                {"documentId":"hist-001","title":"Resolved hostel WiFi issue","description":"Students could not connect to the hostel network after maintenance.","category":"IT Support","subcategory":"WiFi / Network","registrationNumber":"2022BIT001","priority":"HIGH","sentiment":"NEGATIVE","resolutionText":"The network gateway cache was cleared and stable access was restored.","comments":[{"author":"Student","body":"The issue affected assignment uploads for the whole floor."},{"author":"IT Helpdesk","body":"Gateway sessions were reset and the access point was checked after the complaint."}]}
                ]
                """);

        service.importFile(file, true);

        org.mockito.ArgumentCaptor<List<Document>> documentsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(vectorStore).delete(List.of("hist-001"));
        verify(vectorStore).add(documentsCaptor.capture());

        List<Document> documents = documentsCaptor.getValue();
        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getId()).isEqualTo("hist-001");
        assertThat(document.getText()).contains("Comment Notes:");
        assertThat(document.getText()).contains("Student: The issue affected assignment uploads for the whole floor.");
        assertThat(document.getMetadata())
                .containsEntry("commentSummary",
                        "Student: The issue affected assignment uploads for the whole floor. | IT Helpdesk: Gateway sessions were reset and the access point was checked after the complaint.");
    }
}
