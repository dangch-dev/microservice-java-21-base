package pl.co.common.notification;

import java.util.Map;

/**
 * Cross-service notification event envelope.
 */
public record NotificationEvent(
        String userId,
        String topic,
        String title,
        String message,
        ResourceType resourceType,
        String resourceId,
        Map<String, Object> payload,
        String dedupeKey
) {
}
