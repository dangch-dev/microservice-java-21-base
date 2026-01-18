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
@Table(name = "attempt_question_order", indexes = {
        @Index(name = "uk_attempt_question_order_attempt_display_deleted",
                columnList = "attempt_id, display_order, deleted", unique = true),
        @Index(name = "uk_attempt_question_order_attempt_question_deleted",
                columnList = "attempt_id, exam_version_question_id, deleted", unique = true),
        @Index(name = "idx_attempt_question_order_attempt_deleted",
                columnList = "attempt_id, deleted"),
        @Index(name = "idx_attempt_question_order_deleted", columnList = "deleted")
})
public class AttemptQuestionOrder extends BaseEntity {

    @Column(name = "attempt_id", nullable = false, length = 26)
    private String attemptId;

    @Column(name = "exam_version_question_id", nullable = false, length = 26)
    private String examVersionQuestionId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
