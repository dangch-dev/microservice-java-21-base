package pl.co.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.co.common.jpa.BaseEntity;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "exam_attempts", indexes = {
        @Index(name = "idx_exam_attempts_creator_exam_status_deleted",
                columnList = "created_by, exam_version_id, status, deleted"),
        @Index(name = "idx_exam_attempts_exam_creator_status_deleted",
                columnList = "exam_id, created_by, status, deleted"),
        @Index(name = "idx_exam_attempts_exam_version_created_at",
                columnList = "exam_version_id, created_at"),
        @Index(name = "idx_exam_attempts_creator_created_at",
                columnList = "created_by, created_at"),
        @Index(name = "idx_exam_attempts_status_created_at",
                columnList = "status, created_at"),
        @Index(name = "idx_exam_attempts_deleted", columnList = "deleted")
})
public class ExamAttempt extends BaseEntity {

    @Column(name = "exam_id", nullable = false, length = 26)
    private String examId;

    @Column(name = "exam_version_id", nullable = false, length = 26)
    private String examVersionId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "grading_status", length = 30)
    private String gradingStatus;

    @Column(precision = 8, scale = 2)
    private BigDecimal score;

    @Column(name = "max_score", precision = 8, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "percent", precision = 5, scale = 2)
    private BigDecimal percent;
}
