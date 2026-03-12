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
@Table(name = "exam_session_assignments", indexes = {
        @Index(name = "idx_exam_session_assignments_session_deleted", columnList = "session_id, deleted"),
        @Index(name = "idx_exam_session_assignments_user_deleted", columnList = "user_id, deleted"),
        @Index(name = "idx_exam_session_assignments_code_deleted", columnList = "code, deleted"),
        @Index(name = "idx_exam_session_assignments_attempt_deleted", columnList = "attempt_id, deleted")
})
public class ExamSessionAssignment extends BaseEntity {

    @Column(name = "session_id", nullable = false, length = 26)
    private String sessionId;

    @Column(name = "user_id", length = 26)
    private String userId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "access_code", length = 64)
    private String accessCode;

    @Column(name = "attempt_id", length = 26)
    private String attemptId;
}
