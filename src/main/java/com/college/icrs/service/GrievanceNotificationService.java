package com.college.icrs.service;

import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import com.college.icrs.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrievanceNotificationService {

    private final EmailService emailService;

    public void sendSubmissionEmail(User student, Grievance grievance) {
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

    public void sendAssignmentEmail(User student, User faculty, Grievance grievance) {
        if (student == null) return;
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

    public void sendStatusChangeEmail(User student, Grievance grievance, Status from, Status to) {
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

    public void sendCommentEmailToStudent(User student, Grievance grievance, User author, String commentBody) {
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

    public void sendCommentEmailToFaculty(User faculty, Grievance grievance, User author, String commentBody) {
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

    private void sendEmailSafe(String to, String subject, String body) {
        try {
            emailService.sendAsync(to, subject, body);
        } catch (Exception e) {
            log.warn(IcrsLog.event("email.dispatch.failed", "recipient", to, "subject", subject, "reason", e.getClass().getSimpleName()), e);
        }
    }
}
