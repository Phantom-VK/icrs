package com.college.icrs.service;

import com.college.icrs.exception.ResourceNotFoundException;
import com.college.icrs.model.Comment;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Role;
import com.college.icrs.model.Status;
import com.college.icrs.model.User;
import com.college.icrs.rag.EmbeddingService;
import com.college.icrs.repository.CommentRepository;
import com.college.icrs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrievanceCommentServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CommentRepository commentRepository = mock(CommentRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final GrievanceNotificationService grievanceNotificationService = mock(GrievanceNotificationService.class);
    private final EmbeddingService embeddingService = mock(EmbeddingService.class);

    private final GrievanceCommentService service = new GrievanceCommentService(
            userRepository,
            commentRepository,
            passwordEncoder,
            grievanceNotificationService,
            embeddingService
    );

    @Test
    void reindexesResolvedGrievanceWhenCommentIsAdded() {
        User student = user(1L, "student@college.edu", "Student", Role.STUDENT);
        User faculty = user(2L, "faculty@college.edu", "Faculty", Role.FACULTY);
        Grievance grievance = grievance(student, faculty, Status.RESOLVED);

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(77L);
            return saved;
        });

        service.addComment(grievance, student.getEmail(), "Issue is resolved now.");

        verify(embeddingService).indexGrievance(grievance);
    }

    @Test
    void doesNotReindexUnresolvedGrievanceWhenCommentIsAdded() {
        User student = user(1L, "student@college.edu", "Student", Role.STUDENT);
        User faculty = user(2L, "faculty@college.edu", "Faculty", Role.FACULTY);
        Grievance grievance = grievance(student, faculty, Status.IN_PROGRESS);

        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.addComment(grievance, student.getEmail(), "Please review the attached screenshot.");

        verify(embeddingService, never()).indexGrievance(any(Grievance.class));
    }

    @Test
    void rejectsUnknownCommentAuthor() {
        Grievance grievance = grievance(user(1L, "student@college.edu", "Student", Role.STUDENT), null, Status.RESOLVED);
        when(userRepository.findByEmail("missing@college.edu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addComment(grievance, "missing@college.edu", "Hello"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private User user(Long id, String email, String username, Role role) {
        return User.builder()
                .id(id)
                .email(email)
                .username(username)
                .password("secret")
                .role(role)
                .enabled(true)
                .build();
    }

    private Grievance grievance(User student, User faculty, Status status) {
        Grievance grievance = new Grievance();
        grievance.setId(44L);
        grievance.setTitle("WiFi issue");
        grievance.setDescription("WiFi issue");
        grievance.setStudent(student);
        grievance.setAssignedTo(faculty);
        grievance.setStatus(status);
        return grievance;
    }
}
