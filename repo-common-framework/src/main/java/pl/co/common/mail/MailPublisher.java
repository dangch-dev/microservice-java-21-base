package pl.co.common.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publish MailMessage to a Kafka topic so notification-service can send email.
 */
@Component
public class MailPublisher {

    private static final Logger log = LoggerFactory.getLogger(MailPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public MailPublisher(KafkaTemplate<String, String> kafkaTemplate,
                         ObjectMapper objectMapper,
                         @Value("${kafka.topics.mail}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(MailMessage message) {
        if (message == null || message.to() == null || message.to().isBlank()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, message.to(), payload);
        } catch (Exception ex) {
            log.warn("Failed to publish mail message: {}", ex.getMessage());
        }
    }
}
