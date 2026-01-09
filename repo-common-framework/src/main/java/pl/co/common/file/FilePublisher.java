package pl.co.common.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FilePublisher {

    private static final Logger log = LoggerFactory.getLogger(FilePublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public FilePublisher(KafkaTemplate<String, String> kafkaTemplate,
                         ObjectMapper objectMapper,
                         @Value("${storage.kafka.topic:storage.file.commit}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(List<String> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(fileIds);
            kafkaTemplate.send(topic, payload);
        } catch (Exception ex) {
            log.warn("Failed to publish file commit event: {}", ex.getMessage());
        }
    }
}
