package pl.co.realtime.service;

import pl.co.common.notification.NotificationEvent;

public interface NotificationService {
    /**
     * Push a notification event to the WS destination for the target user.
     */
    void deliver(NotificationEvent event);
}
