package pl.co.notification.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.common.notification.NotificationEvent;
import pl.co.notification.dto.NotificationPageResponse;
import pl.co.notification.dto.NotificationResponse;
import pl.co.notification.entity.Notification;
import pl.co.notification.repository.NotificationRepository;
import pl.co.notification.service.NotificationService;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final int MAX_DEDUPE_LENGTH = 150;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void create(NotificationEvent request) {
        if (request == null) {
            throw new IllegalArgumentException("Notification request is required");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("Notification userId is required");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Notification title is required");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Notification message is required");
        }

        String dedupeKey = null;
        if (request.dedupeKey() != null) {
            String normalized = request.dedupeKey();
            if (normalized.isBlank()) {
                return;
            }
            dedupeKey = normalized.length() > MAX_DEDUPE_LENGTH
                    ? normalized.substring(0, MAX_DEDUPE_LENGTH)
                    : normalized;
        }


        if (dedupeKey != null) {
            Optional<Notification> existing = notificationRepository.findByDedupeKey(dedupeKey);
            if (existing.isPresent()) {
                return;
            }
        }

        String payloadJson = null;
        if (request.payload() != null && !request.payload().isEmpty()) {
            try {
                payloadJson = objectMapper.writeValueAsString(request.payload());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize notification payload: {}", e.getMessage());
            }
        }


        Notification notification = Notification.builder()
                .userId(request.userId())
                .action(request.action())
                .title(request.title())
                .message(request.message())
                .resourceId(request.resourceId())
                .payload(payloadJson)
                .isRead(false)
                .isSeen(false)
                .dedupeKey(dedupeKey)
                .build();
        try {
            notificationRepository.save(notification);
        } catch (DataIntegrityViolationException ex) {
            if (dedupeKey != null) {
                notificationRepository.findByDedupeKey(dedupeKey)
                        .orElseThrow(() -> ex);
                return;
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse list(String userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = notificationRepository.findByUserId(userId, pageRequest);
        List<NotificationResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        long unread = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return NotificationPageResponse.builder()
                .items(items)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .unreadCount(unread)
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markSeen(String userId, String notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        boolean changed = false;
        if (!notification.isSeen()) {
            notification.setSeen(true);
            notification.setSeenAt(Instant.now());
            changed = true;
        }
        if (changed) {
            notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(String userId, String notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        boolean changed = false;
        Instant now = Instant.now();
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(now);
            changed = true;
        }
        if (!notification.isSeen()) {
            notification.setSeen(true);
            notification.setSeenAt(now);
            changed = true;
        }
        if (changed) {
            notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllSeen(String userId) {
        Instant now = Instant.now();
        notificationRepository.markAllSeen(userId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationResponse toResponse(Notification notification) {
        Map<String, Object> payload = null;
        String payloadJson = notification.getPayload();
        if (payloadJson != null && !payloadJson.isBlank()) {
            try {
                payload = objectMapper.readValue(payloadJson, MAP_TYPE);
            } catch (Exception ex) {
                log.warn("Failed to parse notification payload: {}", ex.getMessage());
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("raw", payloadJson);
                payload =  fallback;
            }
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .action(notification.getAction())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .resourceId(notification.getResourceId())
                .payload(payload)
                .read(notification.isRead())
                .seen(notification.isSeen())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .seenAt(notification.getSeenAt())
                .dedupeKey(notification.getDedupeKey())
                .build();
    }
}
