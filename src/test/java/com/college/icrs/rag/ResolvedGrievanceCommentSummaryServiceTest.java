package com.college.icrs.rag;

import com.college.icrs.model.Comment;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import com.college.icrs.model.User;
import com.college.icrs.repository.CommentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResolvedGrievanceCommentSummaryServiceTest {

    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final ResolvedGrievanceCommentSummaryService service =
            new ResolvedGrievanceCommentSummaryService(commentRepository);

    @Test
    void summarizesLatestResolvedCommentThreadForEmbedding() {
        Grievance grievance = new Grievance();
        grievance.setId(91L);
        grievance.setStatus(Status.RESOLVED);

        when(commentRepository.findByGrievanceIdOrderByCreatedAtAsc(91L)).thenReturn(List.of(
                comment("Student", "Initial issue report with extra   spaces."),
                comment("Faculty", "Requested additional evidence."),
                comment("Student", "Uploaded the required screenshots."),
                comment("Admin", "Verified the update and closed the grievance.")
        ));

        String summary = service.summarizeForEmbedding(grievance);

        assertThat(summary)
                .contains("Student: Initial issue report with extra spaces.")
                .contains("Faculty: Requested additional evidence.")
                .contains("Admin: Verified the update and closed the grievance.");
    }

    @Test
    void returnsNullForUnresolvedGrievance() {
        Grievance grievance = new Grievance();
        grievance.setId(92L);
        grievance.setStatus(Status.IN_PROGRESS);

        assertThat(service.summarizeForEmbedding(grievance)).isNull();
    }

    private Comment comment(String authorName, String body) {
        User author = new User();
        author.setUsername(authorName);

        Comment comment = new Comment();
        comment.setAuthor(author);
        comment.setBody(body);
        return comment;
    }
}
