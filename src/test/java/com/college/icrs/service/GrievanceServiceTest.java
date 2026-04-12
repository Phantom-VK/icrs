package com.college.icrs.service;

import com.college.icrs.model.Category;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Role;
import com.college.icrs.model.Sentiment;
import com.college.icrs.model.Status;
import com.college.icrs.model.Subcategory;
import com.college.icrs.model.User;
import com.college.icrs.rag.EmbeddingService;
import com.college.icrs.repository.GrievanceRepository;
import com.college.icrs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrievanceServiceTest {

    @Mock
    private GrievanceRepository grievanceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private GrievanceStatusAuditService grievanceStatusAuditService;

    @Mock
    private GrievanceNotificationService grievanceNotificationService;

    @Mock
    private GrievanceCommentService grievanceCommentService;

    @InjectMocks
    private GrievanceService grievanceService;

    @Test
    void shouldCreateGrievanceWithFreshAiStateAndMostSpecificAssignee() {
        User student = user(10L, "student@college.edu", Role.STUDENT);
        User categoryAssignee = user(20L, "hostel@college.edu", Role.FACULTY);
        User subcategoryAssignee = user(21L, "maintenance@college.edu", Role.FACULTY);

        Category category = new Category();
        category.setName("Hostel & Accommodation");
        category.setDefaultAssignee(categoryAssignee);

        Subcategory subcategory = new Subcategory();
        subcategory.setName("Maintenance");
        subcategory.setCategory(category);
        subcategory.setDefaultAssignee(subcategoryAssignee);

        Grievance grievance = new Grievance();
        grievance.setTitle("Water supply issue");
        grievance.setDescription("Water is unavailable in the hostel.");
        grievance.setCategory(category);
        grievance.setSubcategory(subcategory);
        grievance.setAiResolved(true);
        grievance.setAiTitle("Old AI title");
        grievance.setSentiment(Sentiment.NEGATIVE);
        grievance.setAiConfidence(0.91d);
        grievance.setAiDecisionAt(LocalDateTime.now());

        when(userRepository.findById(10L)).thenReturn(Optional.of(student));
        when(grievanceRepository.save(any(Grievance.class))).thenAnswer(invocation -> {
            Grievance saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        Grievance created = grievanceService.createGrievance(grievance, 10L);

        assertThat(created.getStudent()).isSameAs(student);
        assertThat(created.getAssignedTo()).isSameAs(subcategoryAssignee);
        assertThat(created.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(created.isAiResolved()).isFalse();
        assertThat(created.getAiTitle()).isNull();
        assertThat(created.getSentiment()).isNull();
        assertThat(created.getAiConfidence()).isNull();
        assertThat(created.getAiDecisionAt()).isNull();

        verify(embeddingService).indexGrievance(created);
        verify(grievanceNotificationService).sendSubmissionEmail(student, created);
    }

    @Test
    void shouldClearAiResolvedFlagWhenStatusMovesBackToManualFlow() {
        User student = user(10L, "student@college.edu", Role.STUDENT);
        Grievance grievance = new Grievance();
        grievance.setId(40L);
        grievance.setTitle("Resolved issue");
        grievance.setDescription("Already resolved");
        grievance.setStudent(student);
        grievance.setStatus(Status.RESOLVED);
        grievance.setAiResolved(true);

        when(grievanceRepository.findById(40L)).thenReturn(Optional.of(grievance));
        when(grievanceRepository.save(grievance)).thenReturn(grievance);

        Grievance updated = grievanceService.updateGrievanceStatus(40L, Status.IN_PROGRESS);

        assertThat(updated.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(updated.isAiResolved()).isFalse();

        verify(embeddingService).indexGrievance(updated);
        verify(grievanceStatusAuditService)
                .appendStatusHistory(updated, Status.RESOLVED, Status.IN_PROGRESS, null);
        verify(grievanceNotificationService)
                .sendStatusChangeEmail(student, updated, Status.RESOLVED, Status.IN_PROGRESS);
    }

    @Test
    void shouldPersistAiResolutionMetadataWhenMarkedResolvedByAi() {
        User student = user(10L, "student@college.edu", Role.STUDENT);
        Grievance grievance = new Grievance();
        grievance.setId(70L);
        grievance.setTitle("WiFi issue");
        grievance.setDescription("Disconnected often");
        grievance.setStudent(student);
        grievance.setStatus(Status.IN_PROGRESS);

        when(grievanceRepository.findById(70L)).thenReturn(Optional.of(grievance));
        when(grievanceRepository.save(grievance)).thenReturn(grievance);

        Grievance resolved = grievanceService.markResolvedByAi(
                70L,
                "Please reconnect to the campus network.",
                "Routine network issue.",
                0.89d,
                "deepseek-chat",
                "DEEPSEEK_AGENTIC_V1"
        );

        assertThat(resolved.getStatus()).isEqualTo(Status.RESOLVED);
        assertThat(resolved.isAiResolved()).isTrue();
        assertThat(resolved.getAiResolutionText()).isEqualTo("Please reconnect to the campus network.");
        assertThat(resolved.getAiResolutionComment()).isEqualTo("Routine network issue.");
        assertThat(resolved.getAiConfidence()).isEqualTo(0.89d);
        assertThat(resolved.getAiModelName()).isEqualTo("deepseek-chat");
        assertThat(resolved.getAiDecisionSource()).isEqualTo("DEEPSEEK_AGENTIC_V1");
        assertThat(resolved.getAiDecisionAt()).isNotNull();

        verify(embeddingService).indexGrievance(resolved);
        verify(grievanceStatusAuditService)
                .appendStatusHistory(resolved, Status.IN_PROGRESS, Status.RESOLVED, "Resolved by AI");
        verify(grievanceNotificationService)
                .sendStatusChangeEmail(student, resolved, Status.IN_PROGRESS, Status.RESOLVED);
    }

    private User user(Long id, String email, Role role) {
        return User.builder()
                .id(id)
                .username(email)
                .email(email)
                .password("secret")
                .role(role)
                .enabled(true)
                .build();
    }
}
