package com.college.icrs.rag;

import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Grievance;
import org.springframework.ai.vectorstore.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final IcrsProperties properties;
    private final GrievanceVectorDocumentFactory documentFactory;

    public void indexGrievance(Grievance grievance) {
        if (!properties.getAi().getRag().isEnabled() || grievance == null) {
            return;
        }

        if (grievance.getId() <= 0L) {
            log.warn(IcrsLog.event("rag.embedding.skipped", "reason", "missing-grievance-id"));
            return;
        }

        try {
            String documentId = String.valueOf(grievance.getId());
            vectorStore.add(List.of(documentFactory.fromGrievance(grievance)));
            log.info(IcrsLog.event("rag.embedding.upserted", "grievanceId", grievance.getId(), "documentId", documentId));
        } catch (Exception e) {
            log.error(IcrsLog.event("rag.embedding.upsert.failed", "grievanceId", grievance.getId()), e);
        }
    }

    public void removeGrievance(Long grievanceId) {
        if (!properties.getAi().getRag().isEnabled() || grievanceId == null) {
            return;
        }

        try {
            vectorStore.delete(List.of(String.valueOf(grievanceId)));
            log.info(IcrsLog.event("rag.embedding.deleted", "grievanceId", grievanceId));
        } catch (Exception e) {
            log.error(IcrsLog.event("rag.embedding.delete.failed", "grievanceId", grievanceId), e);
        }
    }

    public String buildEmbeddingText(Grievance grievance) {
        return grievance == null
                ? ""
                : documentFactory.buildContent(
                grievance.getTitle(),
                grievance.getDescription(),
                grievance.getCategory() != null ? grievance.getCategory().getName() : null,
                grievance.getSubcategory() != null ? grievance.getSubcategory().getName() : null,
                grievance.getRegistrationNumber()
        );
    }
}
