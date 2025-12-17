package pl.co.realtime.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import pl.co.common.notification.NotificationEvent;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationKafkaToWsListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaToWsListener.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${notification.kafka.topic:notification.events}", groupId = "${notification.kafka.group:realtime-noti}")
    public void onMessage(String message) {
        try {
            NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "notification.created");
            payload.put("data", event);
            messagingTemplate.convertAndSendToUser(event.userId(), "/queue/notifications", payload);
        } catch (Exception ex) {
            log.warn("Failed to bridge Kafka->WS: {}", ex.getMessage());
        }
    }
}
