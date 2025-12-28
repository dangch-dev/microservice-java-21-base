package pl.co.realtime.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pl.co.common.notification.NotificationEvent;
import pl.co.realtime.service.NotificationService;


@Component
@RequiredArgsConstructor
public class NotificationKafkaToWsListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaToWsListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${notification.kafka.topic:notification.events}", groupId = "${notification.kafka.group:realtime-noti}")
    public void onMessage(String message) {
        try {
            NotificationEvent event = objectMapper.readValue(message, NotificationEvent.class);
            notificationService.deliver(event);
        } catch (Exception ex) {
            log.warn("Failed to bridge Kafka->WS: {}", ex.getMessage());
        }
    }
}
