package com.greencart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@greencart.local}")
    private String from;

    @Value("${app.mail.support-to:support@greencart.local}")
    private String supportTo;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public void sendPasswordReset(String to, String token) {
        String resetUrl = baseUrl + "/reset-password?token=" + token;
        String subject = "Green Cart password reset";
        String body = "A password reset was requested for your Green Cart account.\n\n"
                + "Reset your password using this link:\n" + resetUrl
                + "\n\nThis link expires in 1 hour. If you did not request this, you can ignore this email.";
        sendOrLog(to, subject, body, "Password reset link: " + resetUrl);
    }

    public void notifySupport(String name, String email, String subject, String message) {
        String body = "New Green Cart contact message\n\n"
                + "Name: " + name + "\n"
                + "Email: " + email + "\n"
                + "Subject: " + subject + "\n\n"
                + message;
        sendOrLog(supportTo, "Green Cart contact: " + subject, body,
                "Contact notification from " + email + ": " + subject);
    }


    /** Used by Member 3's Observer Pattern when an order lifecycle state changes. */
    public void sendOrderStatusUpdate(String to, String customerName, Long orderId, String status, String note) {
        String safeName = customerName == null || customerName.isBlank() ? "Customer" : customerName;
        String subject = "Green Cart order #" + orderId + " - " + status;
        String body = "Hello " + safeName + ",\n\n"
                + "Your Green Cart order #" + orderId + " is now " + status + "."
                + (note == null || note.isBlank() ? "" : "\n\n" + note)
                + "\n\nYou can view the latest status from My Orders.";
        sendOrLog(to, subject, body,
                "Order #" + orderId + " status notification for " + to + ": " + status);
    }

    public void sendSupportResponse(String to, String subject, String response) {
        String body = "Green Cart support has responded to your message.\n\n"
                + response + "\n\nThank you for contacting Green Cart.";
        sendOrLog(to, "Green Cart support: " + subject, body,
                "Support response queued for " + to + ": " + subject);
    }

    private void sendOrLog(String to, String subject, String body, String fallbackLog) {
        if (!mailEnabled) {
            log.info("EMAIL DISABLED - {}", fallbackLog);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Could not send email to {}: {}", to, ex.getMessage());
            throw new IllegalStateException("Email could not be sent. Please check SMTP settings.");
        }
    }
}
