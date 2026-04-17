package pl.co.notification.service.impl;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import pl.co.common.mail.MailMessage;
import pl.co.notification.service.MailService;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    @Value("${spring.mail.from-name}")
    private String fromName;

    @Override
    public void send(MailMessage message) {
        if (message == null || message.to() == null || message.to().isBlank()) {
            return;
        }
        try {
            String subject = message.subject() == null ? "" : message.subject();
            String body = message.body() == null ? "" : message.body();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(message.to());
            helper.setSubject(subject);
            helper.setText(body, message.html());

            log.info("Sending mail via Brevo SMTP. to={}, subject={}, from={} <{}>",
                    message.to(), subject, fromName, from);
            mailSender.send(mimeMessage);
            log.info("Mail sent via Brevo SMTP. to={}, subject={}", message.to(), subject);
        } catch (Exception ex) {
            // Keep pipeline resilient but do log full send failure details for troubleshooting.
            log.error("Failed to send mail via Brevo SMTP. to={}, subject={}, from={} <{}>",
                    message.to(), message.subject(), fromName, from, ex);
        }
    }
}
