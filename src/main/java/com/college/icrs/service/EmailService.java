package com.college.icrs.service;


import com.college.icrs.logging.IcrsLog;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    private final SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();

    @Value("${notifications.enabled:true}")
    private boolean notificationsEnabled;

    public void sendVerificationEmail(String to, String subject, String text) throws MessagingException{
        if (!notificationsEnabled) return;
        log.info(IcrsLog.event("email.verification.send", "recipient", to, "subject", subject));
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, true);

        emailSender.send(message);
    }

    public void sendAsync(String to, String subject, String bodyHtml) {
        if (!notificationsEnabled) {
            log.info(IcrsLog.event("email.async.skipped", "recipient", to, "subject", subject, "reason", "notifications-disabled"));
            return;
        }
        log.info(IcrsLog.event("email.async.queued", "recipient", to, "subject", subject));
        executor.execute(() -> {
            try {
                sendHtml(to, subject, bodyHtml);
            } catch (MessagingException e) {
                log.warn(IcrsLog.event("email.async.failed", "recipient", to, "subject", subject, "reason", e.getClass().getSimpleName()), e);
            }
        });
    }

    public void sendHtml(String to, String subject, String bodyHtml) throws MessagingException {
        log.debug(IcrsLog.event("email.html.send", "recipient", to, "subject", subject));
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(bodyHtml, true);
        emailSender.send(message);
        log.info(IcrsLog.event("email.html.sent", "recipient", to, "subject", subject));
    }
}
