package pl.co.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.co.common.jpa.BaseEntity;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification",
        indexes = {
                @Index(name = "idx_notification_user_created", columnList = "userId, createdAt")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_dedupe", columnNames = "dedupeKey")
        })
public class Notification extends BaseEntity {

    @Column(nullable = false, length = 26)
    private String userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(length = 80)
    private String resourceType;

    @Column(length = 80)
    private String resourceId;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "is_seen", nullable = false)
    private boolean isSeen;

    private Instant readAt;

    private Instant seenAt;

    @Column(length = 150)
    private String dedupeKey;
}
