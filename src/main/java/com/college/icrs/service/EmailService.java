package com.college.icrs.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    private final SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();

    @Value("${notifications.enabled:true}")
    private boolean notificationsEnabled;

    public void sendVerificationEmail(String to, String subject, String text) throws MessagingException{
        if (!notificationsEnabled) return;
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, true);

        emailSender.send(message);
    }

    public void sendAsync(String to, String subject, String bodyHtml) {
        if (!notificationsEnabled) return;
        executor.execute(() -> {
            try {
                sendHtml(to, subject, bodyHtml);
            } catch (MessagingException e) {
                throw new RuntimeException("Failed to send email to " + to + ": " + e.getMessage(), e);
            }
        });
    }

    public void sendHtml(String to, String subject, String bodyHtml) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(bodyHtml, true);
        emailSender.send(message);
    }
}
