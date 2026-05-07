package com.vdt.authservice.external.notification.email;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService implements IEmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    String fromEmail;

    @Value("${app.security.cors-allowed-origins}")
    String frontendBaseUrl;

    @Async
    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true để gửi dưới dạng HTML

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {} | Error: {}", to, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendActivationEmail(String to, String token) {
        String subject = "Account Activation";
        String link = frontendBaseUrl + "/activate?token=" + token;
        String body = "<h1>Welcome to VDT!</h1>" +
                      "<p>Click the link below to activate your account:</p>" +
                      "<a href=\"" + link + "\" style=\"display: inline-block; padding: 10px 20px; background-color: #4F46E5; color: white; text-decoration: none; border-radius: 5px;\">Activate Account</a>" +
                      "<p>If you can't click the button, copy this link: " + link + "</p>";
        sendEmail(to, subject, body);
    }

    @Async
    @Override
    public void sendResetPasswordEmail(String to, String token) {
        String subject = "Reset Password Request";
        String link = frontendBaseUrl + "/reset-password?token=" + token;
        String body = "<h1>Reset Your Password</h1>" +
                      "<p>We received a request to reset your password. Click the button below to set a new password:</p>" +
                      "<a href=\"" + link + "\" style=\"display: inline-block; padding: 10px 20px; background-color: #4F46E5; color: white; text-decoration: none; border-radius: 5px;\">Reset Password</a>" +
                      "<p>If you didn't request this, please ignore this email.</p>";
        sendEmail(to, subject, body);
    }
}
