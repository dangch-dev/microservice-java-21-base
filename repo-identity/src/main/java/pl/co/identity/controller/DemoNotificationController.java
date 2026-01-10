package pl.co.identity.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.co.common.notification.NotificationAction;
import pl.co.common.notification.NotificationEvent;
import pl.co.common.notification.NotificationPublisher;

import java.util.Map;

/**
 * Demo endpoint để bắn thử notification qua Kafka/realtime.
 * Chỉ dùng cho manual test.
 */
@RestController
@RequestMapping("/demo/notifications")
@RequiredArgsConstructor
public class DemoNotificationController {

    private final NotificationPublisher notificationPublisher;

    @PostMapping("/{userId}")
    public void sendDemo(@PathVariable String userId) {
        NotificationEvent event = new NotificationEvent(
                userId,
                NotificationAction.TICKET_ASSIGNED.name(),
                "Demo notification",
                "This is a demo notification from identity service",
                null,
                Map.of("source", "identity-demo"),
                "identity-demo:" + userId
        );
        notificationPublisher.publish(event);
    }
}
