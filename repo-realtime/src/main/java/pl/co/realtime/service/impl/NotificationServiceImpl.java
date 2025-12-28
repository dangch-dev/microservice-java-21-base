package pl.co.realtime.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.co.common.notification.NotificationEvent;
import pl.co.realtime.service.NotificationService;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void deliver(NotificationEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "notification.created");
        payload.put("data", event);
        messagingTemplate.convertAndSendToUser(event.userId(), "/queue/notifications", payload);
    }
}
