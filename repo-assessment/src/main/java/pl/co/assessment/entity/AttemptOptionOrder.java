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
@Table(name = "attempt_option_order", indexes = {
        @Index(name = "idx_attempt_option_order_attempt_question_deleted",
                columnList = "attempt_id, question_version_id, deleted"),
        @Index(name = "uk_attempt_option_order_attempt_question_display_deleted",
                columnList = "attempt_id, question_version_id, display_order, deleted", unique = true),
        @Index(name = "uk_attempt_option_order_attempt_question_option_deleted",
                columnList = "attempt_id, question_version_id, option_key, deleted", unique = true),
        @Index(name = "idx_attempt_option_order_attempt_deleted",
                columnList = "attempt_id, deleted"),
        @Index(name = "idx_attempt_option_order_deleted", columnList = "deleted")
})
public class AttemptOptionOrder extends BaseEntity {

    @Column(name = "attempt_id", nullable = false, length = 26)
    private String attemptId;

    @Column(name = "question_version_id", nullable = false, length = 26)
    private String questionVersionId;

    @Column(name = "option_key", nullable = false, length = 50)
    private String optionKey;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
