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
import pl.co.assessment.entity.converter.AnswerJsonConverter;
import pl.co.assessment.entity.json.AnswerJson;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_answers", indexes = {
        @Index(name = "uk_user_answers_attempt_question_deleted",
                columnList = "attempt_id, exam_version_question_id, deleted", unique = true),
        @Index(name = "idx_user_answers_attempt_deleted", columnList = "attempt_id, deleted"),
        @Index(name = "idx_user_answers_grading_status_graded_at",
                columnList = "grading_status, graded_at"),
        @Index(name = "idx_user_answers_grader_deleted", columnList = "grader_id, deleted"),
        @Index(name = "idx_user_answers_deleted", columnList = "deleted")
})
public class UserAnswer extends BaseEntity {

    @Column(name = "attempt_id", nullable = false, length = 26)
    private String attemptId;

    @Column(name = "exam_version_question_id", nullable = false, length = 26)
    private String examVersionQuestionId;

    @Convert(converter = AnswerJsonConverter.class)
    @Column(name = "answer_json", nullable = true, columnDefinition = "json")
    private AnswerJson answerJson;

    @Column(name = "earned_points", precision = 8, scale = 2)
    private BigDecimal earnedPoints;

    @Column(name = "grading_status", nullable = false, length = 30)
    private String gradingStatus;

    @Column(name = "grader_id", length = 100)
    private String graderId;

    @Column(name = "graded_at")
    private Instant gradedAt;
}
