package pl.co.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.co.common.mail.MailMessage;
import pl.co.notification.service.MailService;

@Component
@RequiredArgsConstructor
public class MailKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(MailKafkaListener.class);
    private final MailService mailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.mail}",
            groupId = "${notification.kafka.group}")
    public void onMessage(String message) {
        try {
            MailMessage mailMessage = objectMapper.readValue(message, MailMessage.class);
            mailService.send(mailMessage);
        } catch (Exception ex) {
            log.warn("Failed to process mail kafka event: {}", ex.getMessage());
        }
    }
}
