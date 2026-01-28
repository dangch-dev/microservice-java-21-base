package pl.co.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String topic, String key, Object payload) {
        if (topic == null || topic.isBlank()) {
            return;
        }
        try {
            String message = payload == null ? null : objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, message);
        } catch (Exception ex) {
            log.warn("Failed to publish common event: {}", ex.getMessage());
        }
    }
}
