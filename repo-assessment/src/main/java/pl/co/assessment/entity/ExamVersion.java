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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "exam_versions", indexes = {
        @Index(name = "uk_exam_versions_exam_id_version_deleted", columnList = "exam_id, version, deleted", unique = true),
        @Index(name = "idx_exam_versions_exam_id_status_deleted", columnList = "exam_id, status, deleted"),
        @Index(name = "idx_exam_versions_deleted", columnList = "deleted")
})
public class ExamVersion extends BaseEntity {

    @Column(name = "exam_id", nullable = false, length = 26)
    private String examId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "shuffle_questions", nullable = false)
    private boolean shuffleQuestions;

    @Column(name = "shuffle_options", nullable = false)
    private boolean shuffleOptions;
}
