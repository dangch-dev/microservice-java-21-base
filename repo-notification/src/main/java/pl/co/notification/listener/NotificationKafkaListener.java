package pl.co.notification.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.co.common.notification.NotificationEvent;
import pl.co.notification.service.NotificationService;

@Component
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaListener.class);
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${notification.kafka.topic:notification.events}",
            groupId = "${notification.kafka.group:notification-service}")
    public void onMessage(String message) {
        try {
            NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
            notificationService.create(event);
        } catch (Exception ex) {
            log.warn("Failed to process notification kafka event: {}", ex.getMessage());
        }
    }
}
