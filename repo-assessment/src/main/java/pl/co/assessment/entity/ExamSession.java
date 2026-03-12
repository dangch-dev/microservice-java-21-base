package pl.co.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
@Table(name = "exam_sessions", indexes = {
        @Index(name = "idx_exam_sessions_exam_deleted", columnList = "exam_id, deleted"),
        @Index(name = "idx_exam_sessions_target_deleted", columnList = "target_type, deleted"),
        @Index(name = "idx_exam_sessions_code_deleted", columnList = "code, deleted")
})
public class ExamSession extends BaseEntity {

    @Column(name = "exam_id", nullable = false, length = 26)
    private String examId;

    @Column(length = 255)
    private String title;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private ExamSessionTargetType targetType;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "access_code", length = 64)
    private String accessCode;
}
