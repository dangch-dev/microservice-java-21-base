package pl.co.assessment.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.co.assessment.service.AttemptGradingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttemptGradingListener {

    private final ObjectMapper objectMapper;
    private final AttemptGradingService attemptGradingService;

    @KafkaListener(topics = "${kafka.topics.assessment-grading}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        JsonNode payload;
        try {
            payload = objectMapper.readTree(message);
        } catch (Exception ex) {
            log.warn("Invalid grading event payload: {}", ex.getMessage());
            throw new IllegalArgumentException("Invalid grading event payload", ex);
        }

        String attemptId = payload.get("attemptId") == null ? null : payload.get("attemptId").asText();
        if (attemptId == null || attemptId.isBlank()) {
            log.warn("Grading event missing attemptId");
            return;
        }

        attemptGradingService.gradeAttempt(attemptId);
    }
}
