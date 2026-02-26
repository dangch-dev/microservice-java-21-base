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
@Table(name = "question_group_items", indexes = {
        @Index(name = "uk_question_group_items_version_order_deleted",
                columnList = "question_group_version_id, item_order, deleted", unique = true),
        @Index(name = "uk_question_group_items_version_question_deleted",
                columnList = "question_group_version_id, question_version_id, deleted", unique = true),
        @Index(name = "idx_question_group_items_version_deleted",
                columnList = "question_group_version_id, deleted"),
        @Index(name = "idx_question_group_items_deleted", columnList = "deleted")
})
public class QuestionGroupItem extends BaseEntity {

    @Column(name = "question_group_version_id", nullable = false, length = 26)
    private String questionGroupVersionId;

    @Column(name = "question_version_id", nullable = false, length = 26)
    private String questionVersionId;

    @Column(name = "item_order", nullable = false)
    private Integer itemOrder;
}
