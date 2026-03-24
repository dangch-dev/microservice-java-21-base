package pl.co.common.notification;

import pl.co.common.anotation.Trim;

import java.util.Map;

/**
 * Cross-service notification event envelope.
 */
public record NotificationEvent(
        @Trim
        String userId,
        @Trim
        String action,
        @Trim
        String title,
        @Trim
        String message,
        @Trim
        String resourceId,
        Map<String, Object> payload,
        @Trim
        String dedupeKey
) {
}
