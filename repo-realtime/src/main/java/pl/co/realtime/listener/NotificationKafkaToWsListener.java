package pl.co.realtime.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pl.co.common.notification.NotificationEvent;
import pl.co.realtime.service.NotificationService;


@Component
@RequiredArgsConstructor
public class NotificationKafkaToWsListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaToWsListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.notification}", groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(String message,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received mail event from Kafka: topic={}, partition={}, offset={}, message={}", topic, partition, offset,message);
        try {
            NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
            notificationService.deliver(event);
        } catch (Exception ex) {
            log.warn("Failed to bridge Kafka->WS: {}", ex.getMessage());
        }
    }
}
