package pl.co.notification.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.co.notification.entity.Notification;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    Page<Notification> findByUserId(String userId, Pageable pageable);
    Optional<Notification> findByIdAndUserId(String id, String userId);
    Optional<Notification> findByDedupeKey(String dedupeKey);
    long countByUserIdAndIsReadFalse(String userId);

    @Modifying
    @Query("update Notification n set n.isSeen = true, n.seenAt = :seenAt where n.userId = :userId and n.isSeen = false")
    int markAllSeen(@Param("userId") String userId, @Param("seenAt") Instant seenAt);
}
