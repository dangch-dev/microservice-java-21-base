package pl.co.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class NotificationResponse {
    private final String id;
    private final String action;
    private final String title;
    private final String message;
    private final String resourceType;
    private final String resourceId;
    private final Map<String, Object> payload;
    private final boolean read;
    private final boolean seen;
    private final Instant createdAt;
    private final Instant readAt;
    private final Instant seenAt;
    private final String dedupeKey;
}
