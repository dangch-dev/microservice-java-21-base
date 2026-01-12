package pl.co.common.notification;

import java.util.Map;

/**
 * Cross-service notification event envelope.
 */
public record NotificationEvent(
        String userId,
        String action,
        String title,
        String message,
        String resourceId,
        Map<String, Object> payload,
        String dedupeKey
) {
}
