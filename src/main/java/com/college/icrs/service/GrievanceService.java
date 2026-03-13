package com.college.icrs.service;

import com.college.icrs.exception.ForbiddenOperationException;
import com.college.icrs.exception.InvalidRequestException;
import com.college.icrs.exception.ResourceNotFoundException;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Priority;
import com.college.icrs.model.Sentiment;
import com.college.icrs.model.Status;
import com.college.icrs.model.StatusHistory;
import com.college.icrs.model.User;
import com.college.icrs.model.Comment;
import com.college.icrs.repository.GrievanceRepository;
import com.college.icrs.repository.StatusHistoryRepository;
import com.college.icrs.repository.UserRepository;
import com.college.icrs.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@lombok.RequiredArgsConstructor
@Slf4j
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final UserRepository userRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final CommentRepository commentRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public Grievance createGrievance(Grievance grievance, Long studentId) {
        log.info(IcrsLog.event("grievance.create.start", "studentId", studentId, "title", grievance.getTitle()));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        grievance.setStudent(student);
        grievance.setStatus(Status.SUBMITTED);
        initializeAiFieldsForNewGrievance(grievance);

        // auto-assign to default assignee if present on subcategory or category
        if (grievance.getAssignedTo() == null && grievance.getSubcategory() != null && grievance.getSubcategory().getDefaultAssignee() != null) {
            grievance.setAssignedTo(grievance.getSubcategory().getDefaultAssignee());
            grievance.setStatus(Status.IN_PROGRESS);
        } else if (grievance.getAssignedTo() == null && grievance.getCategory() != null && grievance.getCategory().getDefaultAssignee() != null) {
            grievance.setAssignedTo(grievance.getCategory().getDefaultAssignee());
            grievance.setStatus(Status.IN_PROGRESS);
        }

        Grievance saved = grievanceRepository.save(grievance);
        log.info(IcrsLog.event("grievance.create.completed",
                "grievanceId", saved.getId(),
                "status", saved.getStatus(),
                "assignedTo", saved.getAssignedTo() != null ? saved.getAssignedTo().getEmail() : null));
        trySendSubmissionEmail(student, saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public Grievance getGrievanceById(Long id) {
        return grievanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found with id: " + id));
    }

    public Grievance updateGrievance(Long id, Grievance grievanceDetails) {
        Grievance grievance = getGrievanceById(id);
        Status fromStatus = grievance.getStatus();
        Status targetStatus = fromStatus;

        grievance.setTitle(grievanceDetails.getTitle());
        grievance.setDescription(grievanceDetails.getDescription());
        grievance.setCategory(grievanceDetails.getCategory());
        grievance.setSubcategory(grievanceDetails.getSubcategory());
        if (grievanceDetails.getStatus() != null) {
            targetStatus = grievanceDetails.getStatus();
            reconcileAiFlagsForManualStatusChange(grievance, targetStatus);
            grievance.setStatus(targetStatus);
        }

        Grievance saved = grievanceRepository.save(grievance);
        appendStatusHistory(saved, fromStatus, targetStatus, null);
        if (fromStatus != targetStatus) {
            trySendStatusChangeEmail(grievance.getStudent(), saved, fromStatus, targetStatus);
        }
        return saved;
    }

    public void deleteGrievance(Long id) {
        Grievance grievance = getGrievanceById(id);
        grievanceRepository.delete(grievance);
    }

    @Transactional(readOnly = true)
    public Page<Grievance> getAllGrievances(Pageable pageable) {
        return grievanceRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Grievance> getGrievancesByStudent(Long studentId) {
        return grievanceRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    @Transactional(readOnly = true)
    public Page<Grievance> getGrievancesByStatus(Status status, Pageable pageable) {
        return grievanceRepository.findByStatus(status, pageable);
    }

    public Grievance assignGrievanceToFaculty(Long grievanceId, Long facultyId) {
        Grievance grievance = getGrievanceById(grievanceId);
        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found with id: " + facultyId));

        reconcileAiFlagsForManualStatusChange(grievance, Status.IN_PROGRESS);
        grievance.setAssignedTo(faculty);
        grievance.setStatus(Status.IN_PROGRESS);
        Grievance updated = grievanceRepository.save(grievance);
        trySendAssignmentEmail(grievance.getStudent(), faculty, updated);
        return updated;
    }

    public Grievance updateGrievanceStatus(Long grievanceId, Status status) {
        log.info(IcrsLog.event("grievance.status.update.start", "grievanceId", grievanceId, "targetStatus", status));
        Grievance grievance = getGrievanceById(grievanceId);
        Status fromStatus = grievance.getStatus();
        reconcileAiFlagsForManualStatusChange(grievance, status);
        grievance.setStatus(status);
        Grievance saved = grievanceRepository.save(grievance);
        appendStatusHistory(saved, fromStatus, status, null);

        trySendStatusChangeEmail(grievance.getStudent(), saved, fromStatus, status);
        log.info(IcrsLog.event("grievance.status.update.completed",
                "grievanceId", grievanceId,
                "fromStatus", fromStatus,
                "toStatus", status));
        return saved;
    }

    public Grievance applyAiDecisionMetadata(
            Long grievanceId,
            Priority priority,
            Sentiment sentiment,
            String aiTitle,
            Double aiConfidence,
            String aiModelName,
            String aiDecisionSource,
            LocalDateTime aiDecisionAt
    ) {
        log.info(IcrsLog.event("grievance.ai.metadata.update.start", "grievanceId", grievanceId));
        Grievance grievance = getGrievanceById(grievanceId);
        grievance.setPriority(priority);
        grievance.setSentiment(sentiment);
        grievance.setAiTitle(aiTitle);
        grievance.setAiConfidence(aiConfidence);
        grievance.setAiModelName(aiModelName);
        grievance.setAiDecisionSource(aiDecisionSource);
        grievance.setAiDecisionAt(aiDecisionAt != null ? aiDecisionAt : LocalDateTime.now());
        grievance.setAiResolved(false);
        Grievance saved = grievanceRepository.save(grievance);
        log.info(IcrsLog.event("grievance.ai.metadata.update.completed",
                "grievanceId", grievanceId,
                "priority", priority,
                "sentiment", sentiment,
                "confidence", aiConfidence));
        return saved;
    }

    public Grievance updateAiRecommendation(
            Long grievanceId,
            String aiResolutionText,
            String aiResolutionComment,
            Double aiConfidence,
            String aiModelName,
            String aiDecisionSource,
            LocalDateTime aiDecisionAt
    ) {
        log.info(IcrsLog.event("grievance.ai.manual-review.start", "grievanceId", grievanceId));
        Grievance grievance = getGrievanceById(grievanceId);
        Status currentStatus = grievance.getStatus();
        grievance.setAiResolved(false);
        grievance.setAiResolutionText(aiResolutionText);
        grievance.setAiResolutionComment(aiResolutionComment);
        if (aiConfidence != null) {
            grievance.setAiConfidence(aiConfidence);
        }
        grievance.setAiModelName(aiModelName);
        grievance.setAiDecisionSource(aiDecisionSource);
        grievance.setAiDecisionAt(aiDecisionAt != null ? aiDecisionAt : LocalDateTime.now());
        Grievance saved = grievanceRepository.save(grievance);
        appendStatusHistory(saved, currentStatus, currentStatus, "Redirected to the corresponding faculty by AI");
        log.info(IcrsLog.event("grievance.ai.manual-review.completed",
                "grievanceId", grievanceId,
                "status", saved.getStatus(),
                "confidence", grievance.getAiConfidence()));
        return saved;
    }

    public Grievance markResolvedByAi(
            Long grievanceId,
            String aiResolutionText,
            String aiResolutionComment,
            Double aiConfidence,
            String aiModelName,
            String aiDecisionSource
    ) {
        log.info(IcrsLog.event("grievance.ai.resolve.start", "grievanceId", grievanceId));
        Grievance grievance = getGrievanceById(grievanceId);
        Status fromStatus = grievance.getStatus();

        grievance.setAiResolved(true);
        grievance.setAiResolutionText(aiResolutionText);
        grievance.setAiResolutionComment(aiResolutionComment);
        grievance.setAiConfidence(aiConfidence);
        grievance.setAiModelName(aiModelName);
        grievance.setAiDecisionSource(aiDecisionSource);
        grievance.setAiDecisionAt(LocalDateTime.now());
        grievance.setStatus(Status.RESOLVED);

        Grievance saved = grievanceRepository.save(grievance);
        appendStatusHistory(saved, fromStatus, Status.RESOLVED, "Resolved by AI");
        trySendStatusChangeEmail(grievance.getStudent(), saved, fromStatus, Status.RESOLVED);
        log.info(IcrsLog.event("grievance.ai.resolve.completed",
                "grievanceId", grievanceId,
                "fromStatus", fromStatus,
                "toStatus", saved.getStatus(),
                "confidence", aiConfidence));
        return saved;
    }

    public com.college.icrs.dto.CommentResponseDTO addSystemComment(
            Long grievanceId,
            String systemAuthorEmail,
            String body
    ) {
        log.info(IcrsLog.event("grievance.system-comment.start", "grievanceId", grievanceId, "authorEmail", systemAuthorEmail));
        Grievance grievance = getGrievanceById(grievanceId);
        User author = ensureSystemAuthor(systemAuthorEmail);

        if (author.getRole() == com.college.icrs.model.Role.STUDENT) {
            throw new InvalidRequestException("System author must not be a student.");
        }

        Comment comment = new Comment();
        comment.setGrievance(grievance);
        comment.setAuthor(author);
        comment.setBody(body);

        Comment saved = commentRepository.save(comment);
        if (grievance.getStudent() != null) {
            trySendCommentEmailToStudent(grievance.getStudent(), grievance, author, body);
        }
        com.college.icrs.dto.CommentResponseDTO dto = new com.college.icrs.dto.CommentResponseDTO();
        dto.setId(saved.getId());
        dto.setBody(saved.getBody());
        dto.setAuthorName(author.getUsername());
        dto.setAuthorEmail(author.getEmail());
        dto.setCreatedAt(saved.getCreatedAt());
        log.info(IcrsLog.event("grievance.system-comment.completed", "grievanceId", grievanceId, "commentId", saved.getId()));
        return dto;
    }

    public com.college.icrs.dto.CommentResponseDTO addComment(Long grievanceId, String authorEmail, String body) {
        log.info(IcrsLog.event("grievance.comment.start", "grievanceId", grievanceId, "authorEmail", authorEmail));
        Grievance grievance = getGrievanceById(grievanceId);
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // permission: students can comment only on their own grievance; faculty/admin allowed
        if (author.getRole() == com.college.icrs.model.Role.STUDENT) {
            if (grievance.getStudent() == null || !grievance.getStudent().getId().equals(author.getId())) {
                throw new ForbiddenOperationException("Students can only comment on their own grievances.");
            }
        }

        Comment comment = new Comment();
        comment.setGrievance(grievance);
        comment.setAuthor(author);
        comment.setBody(body);

        Comment saved = commentRepository.save(comment);

        // notify student if commenter is faculty/admin; notify assigned faculty if commenter is student
        if (author.getRole() != com.college.icrs.model.Role.STUDENT && grievance.getStudent() != null) {
            trySendCommentEmailToStudent(grievance.getStudent(), grievance, author, body);
        } else if (author.getRole() == com.college.icrs.model.Role.STUDENT && grievance.getAssignedTo() != null) {
            trySendCommentEmailToFaculty(grievance.getAssignedTo(), grievance, author, body);
        }

        com.college.icrs.dto.CommentResponseDTO dto = new com.college.icrs.dto.CommentResponseDTO();
        dto.setId(saved.getId());
        dto.setBody(saved.getBody());
        dto.setAuthorName(author.getUsername());
        dto.setAuthorEmail(author.getEmail());
        dto.setCreatedAt(saved.getCreatedAt());
        log.info(IcrsLog.event("grievance.comment.completed", "grievanceId", grievanceId, "commentId", saved.getId(), "authorEmail", authorEmail));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<com.college.icrs.dto.CommentResponseDTO> getComments(Long grievanceId, String requesterEmail) {
        Grievance grievance = getGrievanceById(grievanceId);
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (requester.getRole() == com.college.icrs.model.Role.STUDENT) {
            if (grievance.getStudent() == null || !grievance.getStudent().getId().equals(requester.getId())) {
                throw new ForbiddenOperationException("Students can only view their own grievances.");
            }
        }

        return commentRepository.findByGrievanceIdOrderByCreatedAtAsc(grievanceId)
                .stream()
                .map(c -> {
                    com.college.icrs.dto.CommentResponseDTO dto = new com.college.icrs.dto.CommentResponseDTO();
                    dto.setId(c.getId());
                    dto.setBody(c.getBody());
                    if (c.getAuthor() != null) {
                        dto.setAuthorName(c.getAuthor().getUsername());
                        dto.setAuthorEmail(c.getAuthor().getEmail());
                    }
                    dto.setCreatedAt(c.getCreatedAt());
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getGrievanceStatistics() {
        Map<String, Long> stats = new HashMap<>();
        long resolvedCount = grievanceRepository.countByStatus(Status.RESOLVED);
        long aiResolvedCount = grievanceRepository.countByAiResolvedTrue();
        stats.put("total", grievanceRepository.count());
        stats.put("submitted", grievanceRepository.countByStatus(Status.SUBMITTED));
        stats.put("inProgress", grievanceRepository.countByStatus(Status.IN_PROGRESS));
        stats.put("resolved", resolvedCount);
        stats.put("aiResolved", aiResolvedCount);
        stats.put("humanResolved", Math.max(resolvedCount - aiResolvedCount, 0));
        return stats;
    }

    private void initializeAiFieldsForNewGrievance(Grievance grievance) {
        grievance.setPriority(null);
        grievance.setSentiment(null);
        grievance.setAiResolved(false);
        grievance.setAiConfidence(null);
        grievance.setAiTitle(null);
        grievance.setAiResolutionText(null);
        grievance.setAiResolutionComment(null);
        grievance.setAiModelName(null);
        grievance.setAiDecisionAt(null);
        grievance.setAiDecisionSource(null);
    }

    private void reconcileAiFlagsForManualStatusChange(Grievance grievance, Status targetStatus) {
        if (targetStatus != Status.RESOLVED) {
            grievance.setAiResolved(false);
        }
    }

    private void appendStatusHistory(Grievance grievance, Status fromStatus, Status toStatus, String reason) {
        if (fromStatus == toStatus && !StringUtils.hasText(reason)) {
            return;
        }
        StatusHistory history = new StatusHistory();
        history.setGrievance(grievance);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setReason(reason);
        statusHistoryRepository.save(history);
    }

    private User ensureSystemAuthor(String systemAuthorEmail) {
        return userRepository.findByEmail(systemAuthorEmail).orElseGet(() -> {
            log.info(IcrsLog.event("grievance.system-author.create", "email", systemAuthorEmail));
            User systemUser = new User();
            systemUser.setUsername("AI System");
            systemUser.setEmail(systemAuthorEmail);
            systemUser.setPassword(passwordEncoder.encode("ai-system-disabled-login"));
            systemUser.setRole(com.college.icrs.model.Role.ADMIN);
            systemUser.setEnabled(true);
            systemUser.setDepartment("SYSTEM");
            systemUser.setStudentId("AI_SYSTEM");
            return userRepository.save(systemUser);
        });
    }

    private void trySendSubmissionEmail(User student, Grievance grievance) {
        if (student == null) return;
        log.info(IcrsLog.event("email.submission.prepare", "grievanceId", grievance.getId(), "studentEmail", student.getEmail()));
        String subject = "Grievance submitted: " + grievance.getTitle();
        String body = """
                <p>Dear %s,</p>
                <p>Your grievance "<b>%s</b>" has been submitted with status <b>%s</b>.</p>
                <p>Registration number: %s</p>
                """.formatted(student.getUsername(), grievance.getTitle(), grievance.getStatus(), grievance.getRegistrationNumber());
        sendEmailSafe(student.getEmail(), subject, body);
    }

    private void trySendAssignmentEmail(User student, User faculty, Grievance grievance) {
        if (student != null) {
            log.info(IcrsLog.event("email.assignment.prepare",
                    "grievanceId", grievance.getId(),
                    "studentEmail", student.getEmail(),
                    "facultyEmail", faculty != null ? faculty.getEmail() : null));
            String subject = "Grievance assigned: " + grievance.getTitle();
            String body = """
                    <p>Dear %s,</p>
                    <p>Your grievance "<b>%s</b>" has been assigned to <b>%s</b> and is now <b>%s</b>.</p>
                    """.formatted(student.getUsername(), grievance.getTitle(),
                    faculty != null ? faculty.getUsername() : "faculty", grievance.getStatus());
            sendEmailSafe(student.getEmail(), subject, body);
        }
    }

    private void trySendStatusChangeEmail(User student, Grievance grievance, Status from, Status to) {
        if (student == null) return;
        log.info(IcrsLog.event("email.status-change.prepare",
                "grievanceId", grievance.getId(),
                "studentEmail", student.getEmail(),
                "fromStatus", from,
                "toStatus", to));
        String subject = "Grievance status updated: " + grievance.getTitle();
        String body = """
                <p>Dear %s,</p>
                <p>The status of your grievance "<b>%s</b>" changed from <b>%s</b> to <b>%s</b>.</p>
                """.formatted(student.getUsername(), grievance.getTitle(), from, to);
        sendEmailSafe(student.getEmail(), subject, body);
    }

    private void sendEmailSafe(String to, String subject, String body) {
        try {
            emailService.sendAsync(to, subject, body);
        } catch (Exception e) {
            log.warn(IcrsLog.event("email.dispatch.failed", "recipient", to, "subject", subject, "reason", e.getClass().getSimpleName()), e);
        }
    }

    private void trySendCommentEmailToStudent(User student, Grievance grievance, User author, String commentBody) {
        log.info(IcrsLog.event("email.comment-to-student.prepare",
                "grievanceId", grievance.getId(),
                "studentEmail", student.getEmail(),
                "authorEmail", author.getEmail()));
        String subject = "New comment on your grievance: " + grievance.getTitle();
        String body = """
                <p>Dear %s,</p>
                <p>%s added a comment on your grievance "<b>%s</b>":</p>
                <blockquote>%s</blockquote>
                """.formatted(student.getUsername(), author.getUsername(), grievance.getTitle(), commentBody);
        sendEmailSafe(student.getEmail(), subject, body);
    }

    private void trySendCommentEmailToFaculty(User faculty, Grievance grievance, User author, String commentBody) {
        log.info(IcrsLog.event("email.comment-to-faculty.prepare",
                "grievanceId", grievance.getId(),
                "facultyEmail", faculty.getEmail(),
                "authorEmail", author.getEmail()));
        String subject = "New student comment on grievance: " + grievance.getTitle();
        String body = """
                <p>Hello %s,</p>
                <p>%s commented on grievance "<b>%s</b>":</p>
                <blockquote>%s</blockquote>
                """.formatted(faculty.getUsername(), author.getUsername(), grievance.getTitle(), commentBody);
        sendEmailSafe(faculty.getEmail(), subject, body);
    }
}
