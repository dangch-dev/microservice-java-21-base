package pl.co.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.co.assessment.entity.converter.GroupPromptContentConverter;
import pl.co.assessment.entity.json.GroupPromptContent;
import pl.co.common.jpa.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "question_group_versions", indexes = {
        @Index(name = "idx_question_group_versions_group_created_at",
                columnList = "question_group_id, created_at"),
        @Index(name = "idx_question_group_versions_deleted", columnList = "deleted")
})
public class QuestionGroupVersion extends BaseEntity {

    @Column(name = "question_group_id", nullable = false, length = 26)
    private String questionGroupId;

    @Convert(converter = GroupPromptContentConverter.class)
    @Column(name = "prompt_content", nullable = false, columnDefinition = "json")
    private GroupPromptContent promptContent;
}
