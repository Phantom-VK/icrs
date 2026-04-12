package com.college.icrs.rag;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.model.Category;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import com.college.icrs.model.Subcategory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddingServiceTest {

    @Test
    void indexesResolvedGrievanceWithCommentSummaryInVectorDocument() {
        VectorStore vectorStore = mock(VectorStore.class);
        ResolvedGrievanceCommentSummaryService commentSummaryService = mock(ResolvedGrievanceCommentSummaryService.class);
        GrievanceVectorDocumentFactory documentFactory = new GrievanceVectorDocumentFactory();
        IcrsProperties properties = new IcrsProperties();
        properties.getAi().getRag().setEnabled(true);

        EmbeddingService service = new EmbeddingService(
                vectorStore,
                properties,
                documentFactory,
                commentSummaryService
        );

        Grievance grievance = grievance();
        when(commentSummaryService.summarizeForEmbedding(grievance))
                .thenReturn("Student: Issue persisted after first fix. | Admin: Verified final resolution.");

        service.indexGrievance(grievance);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documentsCaptor.capture());
        Document document = documentsCaptor.getValue().getFirst();

        assertThat(document.getText()).contains("Comment Notes: Student: Issue persisted after first fix. | Admin: Verified final resolution.");
        assertThat(document.getMetadata()).containsEntry(
                GrievanceVectorDocumentFactory.COMMENT_SUMMARY_METADATA_KEY,
                "Student: Issue persisted after first fix. | Admin: Verified final resolution."
        );
    }

    private Grievance grievance() {
        Category category = new Category();
        category.setName("IT Support");

        Subcategory subcategory = new Subcategory();
        subcategory.setName("WiFi / Network");
        subcategory.setCategory(category);

        Grievance grievance = new Grievance();
        grievance.setId(55L);
        grievance.setTitle("Hostel WiFi unstable");
        grievance.setDescription("The hostel WiFi drops every evening.");
        grievance.setCategory(category);
        grievance.setSubcategory(subcategory);
        grievance.setRegistrationNumber("2023BIT055");
        grievance.setStatus(Status.RESOLVED);
        grievance.setAiResolved(true);
        grievance.setAiResolutionText("The access point firmware was updated and the gateway cache was cleared.");
        return grievance;
    }
}
