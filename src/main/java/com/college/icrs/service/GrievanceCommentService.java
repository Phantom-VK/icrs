package com.college.icrs.service;

import com.college.icrs.dto.CommentResponseDTO;
import com.college.icrs.exception.ForbiddenOperationException;
import com.college.icrs.exception.InvalidRequestException;
import com.college.icrs.exception.ResourceNotFoundException;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Comment;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Role;
import com.college.icrs.model.User;
import com.college.icrs.repository.CommentRepository;
import com.college.icrs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceCommentService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;
    private final GrievanceNotificationService grievanceNotificationService;

    public CommentResponseDTO addSystemComment(Grievance grievance, String systemAuthorEmail, String body) {
        log.info(IcrsLog.event("grievance.system-comment.start", "grievanceId", grievance.getId(), "authorEmail", systemAuthorEmail));
        User author = ensureSystemAuthor(systemAuthorEmail);

        if (author.getRole() == Role.STUDENT) {
            throw new InvalidRequestException("System author must not be a student.");
        }

        Comment saved = saveComment(grievance, author, body);
        if (grievance.getStudent() != null) {
            grievanceNotificationService.sendCommentEmailToStudent(grievance.getStudent(), grievance, author, body);
        }

        CommentResponseDTO dto = toCommentResponse(saved);
        log.info(IcrsLog.event("grievance.system-comment.completed", "grievanceId", grievance.getId(), "commentId", saved.getId()));
        return dto;
    }

    public CommentResponseDTO addComment(Grievance grievance, String authorEmail, String body) {
        log.info(IcrsLog.event("grievance.comment.start", "grievanceId", grievance.getId(), "authorEmail", authorEmail));
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        validateCommentAuthor(grievance, author);
        Comment saved = saveComment(grievance, author, body);
        notifyCommentParticipants(grievance, author, body);

        CommentResponseDTO dto = toCommentResponse(saved);
        log.info(IcrsLog.event("grievance.comment.completed", "grievanceId", grievance.getId(), "commentId", saved.getId(), "authorEmail", authorEmail));
        return dto;
    }

    public List<CommentResponseDTO> getComments(Grievance grievance, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (requester.getRole() == Role.STUDENT
                && (grievance.getStudent() == null || !grievance.getStudent().getId().equals(requester.getId()))) {
            throw new ForbiddenOperationException("Students can only view their own grievances.");
        }

        return commentRepository.findByGrievanceIdOrderByCreatedAtAsc(grievance.getId())
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    private void validateCommentAuthor(Grievance grievance, User author) {
        if (author.getRole() == Role.STUDENT
                && (grievance.getStudent() == null || !grievance.getStudent().getId().equals(author.getId()))) {
            throw new ForbiddenOperationException("Students can only comment on their own grievances.");
        }
    }

    private void notifyCommentParticipants(Grievance grievance, User author, String body) {
        if (author.getRole() != Role.STUDENT && grievance.getStudent() != null) {
            grievanceNotificationService.sendCommentEmailToStudent(grievance.getStudent(), grievance, author, body);
        } else if (author.getRole() == Role.STUDENT && grievance.getAssignedTo() != null) {
            grievanceNotificationService.sendCommentEmailToFaculty(grievance.getAssignedTo(), grievance, author, body);
        }
    }

    private Comment saveComment(Grievance grievance, User author, String body) {
        Comment comment = new Comment();
        comment.setGrievance(grievance);
        comment.setAuthor(author);
        comment.setBody(body);
        return commentRepository.save(comment);
    }

    private User ensureSystemAuthor(String systemAuthorEmail) {
        return userRepository.findByEmail(systemAuthorEmail).orElseGet(() -> {
            log.info(IcrsLog.event("grievance.system-author.create", "email", systemAuthorEmail));
            User systemUser = new User();
            systemUser.setUsername("AI System");
            systemUser.setEmail(systemAuthorEmail);
            systemUser.setPassword(passwordEncoder.encode("ai-system-disabled-login"));
            systemUser.setRole(Role.ADMIN);
            systemUser.setEnabled(true);
            systemUser.setDepartment("SYSTEM");
            systemUser.setStudentId("AI_SYSTEM");
            return userRepository.save(systemUser);
        });
    }

    private CommentResponseDTO toCommentResponse(Comment comment) {
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setId(comment.getId());
        dto.setBody(comment.getBody());
        if (comment.getAuthor() != null) {
            dto.setAuthorName(comment.getAuthor().getUsername());
            dto.setAuthorEmail(comment.getAuthor().getEmail());
        }
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
}
