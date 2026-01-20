package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.co.assessment.entity.ExamVersionQuestion;

import java.util.List;
import java.util.Set;

public interface ExamVersionQuestionRepository extends JpaRepository<ExamVersionQuestion, String> {
    List<ExamVersionQuestion> findByExamVersionIdAndDeletedFalseOrderByQuestionOrderAsc(String examVersionId);

    @Query("""
            SELECT COALESCE(MAX(evq.questionOrder), 0)
              FROM ExamVersionQuestion evq
             WHERE evq.examVersionId = :examVersionId
            """)
    Integer findMaxQuestionOrderByExamVersionId(@Param("examVersionId") String examVersionId);

    @Modifying
    @Query("""
            UPDATE ExamVersionQuestion evq
               SET evq.questionOrder = evq.questionOrder + :offset
             WHERE evq.examVersionId = :examVersionId
               AND evq.deleted = false
               AND evq.questionId IN :questionIds
            """)
    int bumpQuestionOrder(@Param("examVersionId") String examVersionId,
                          @Param("questionIds") Set<String> questionIds,
                          @Param("offset") int offset);
}
