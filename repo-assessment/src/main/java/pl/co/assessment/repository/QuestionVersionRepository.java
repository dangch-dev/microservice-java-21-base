package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.co.assessment.entity.QuestionVersion;

public interface QuestionVersionRepository extends JpaRepository<QuestionVersion, String> {
    @Query("""
            SELECT COALESCE(MAX(qv.version), 0)
            FROM QuestionVersion qv
            WHERE qv.questionId = :questionId
              AND qv.deleted = false
            """)
    int findMaxVersionByQuestionIdAndDeletedFalse(@Param("questionId") String questionId);
}
