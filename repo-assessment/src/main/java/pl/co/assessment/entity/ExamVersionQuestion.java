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
@Table(name = "exam_version_questions", indexes = {
        @Index(name = "uk_exam_version_questions_version_order_deleted",
                columnList = "exam_version_id, question_order, deleted", unique = true),
        @Index(name = "uk_exam_version_questions_version_question_deleted",
                columnList = "exam_version_id, question_version_id, deleted", unique = true),
        @Index(name = "idx_exam_version_questions_version_deleted",
                columnList = "exam_version_id, deleted"),
        @Index(name = "idx_exam_version_questions_deleted", columnList = "deleted")
})
public class ExamVersionQuestion extends BaseEntity {

    @Column(name = "exam_version_id", nullable = false, length = 26)
    private String examVersionId;

    @Column(name = "question_version_id", nullable = false, length = 26)
    private String questionVersionId;

    @Column(name = "question_id", nullable = false, length = 26)
    private String questionId;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;
}
