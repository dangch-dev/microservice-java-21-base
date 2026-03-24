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
import pl.co.common.jpa.BaseEntity;
import pl.co.assessment.entity.converter.GradingRulesConverter;
import pl.co.assessment.entity.converter.QuestionContentConverter;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "question_versions", indexes = {
        @Index(name = "uk_question_versions_question_id_version_deleted",
                columnList = "question_id, version, deleted", unique = true),
        @Index(name = "idx_question_versions_question_id_created_at",
                columnList = "question_id, created_at"),
        @Index(name = "idx_question_versions_type_deleted", columnList = "type, deleted"),
        @Index(name = "idx_question_versions_deleted", columnList = "deleted")
})
public class QuestionVersion extends BaseEntity {

    @Column(name = "question_id", nullable = false, length = 26)
    private String questionId;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 50)
    private String type;

    @Convert(converter = QuestionContentConverter.class)
    @Column(name = "question_content", nullable = false, columnDefinition = "json")
    private QuestionContent questionContent;

    @Convert(converter = GradingRulesConverter.class)
    @Column(name = "grading_rules", columnDefinition = "json")
    private GradingRules gradingRules;
}
