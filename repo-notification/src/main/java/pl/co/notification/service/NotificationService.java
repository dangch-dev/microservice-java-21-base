package pl.co.notification.service;

import pl.co.common.notification.NotificationEvent;
import pl.co.notification.dto.NotificationPageResponse;
import pl.co.notification.dto.NotificationResponse;

public interface NotificationService {
    void create(NotificationEvent request);
    NotificationPageResponse list(String userId, int page, int size);
    NotificationResponse markSeen(String userId, String notificationId);
    NotificationResponse markRead(String userId, String notificationId);
    void markAllSeen(String userId);
    long unreadCount(String userId);
}
