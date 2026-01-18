package pl.co.assessment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import pl.co.assessment.entity.Exam;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, String> {

    Optional<Exam> findByIdAndDeletedFalse(String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM Exam e
            WHERE e.id = :id
              AND e.deleted = false
            """)
    Optional<Exam> findByIdAndDeletedFalseForUpdate(@Param("id") String id);

    @Query("""
            SELECT
                evq.questionId AS questionId,
                evq.questionOrder AS questionOrder,
                qv.id AS questionVersionId,
                qv.type AS type,
                qv.questionContent AS questionContent,
                qv.gradingRules AS gradingRules
            FROM ExamVersionQuestion evq
            LEFT JOIN QuestionVersion qv
              ON qv.id = evq.questionVersionId
             AND qv.deleted = false
            WHERE evq.examVersionId = :examVersionId
              AND evq.deleted = false
            ORDER BY evq.questionOrder
            """)
    List<ExamEditorQuestionRow> findEditorQuestionsByVersionId(@Param("examVersionId") String examVersionId);

    @Query(value = """
            SELECT
                e.id AS examId,
                ev.id AS examVersionId,
                c.name AS categoryName,
                ev.name AS name,
                ev.status AS status,
                ev.durationMinutes AS durationMinutes,
                ev.shuffleQuestions AS shuffleQuestions,
                ev.shuffleOptions AS shuffleOptions
            FROM Exam e
            JOIN ExamVersion ev
              ON ev.id = COALESCE(e.publishedExamVersionId, e.draftExamVersionId)
             AND ev.deleted = false
            LEFT JOIN Category c
              ON c.id = e.categoryId
             AND c.deleted = false
            WHERE e.deleted = false
              AND (
                :categoryId IS NULL
                OR :categoryId = ''
                OR e.categoryId = :categoryId
              )
              AND (
                :searchValue IS NULL
                OR :searchValue = ''
                OR LOWER(ev.name) LIKE LOWER(CONCAT('%', :searchValue, '%'))
                OR LOWER(COALESCE(ev.description, '')) LIKE LOWER(CONCAT('%', :searchValue, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(e)
            FROM Exam e
            JOIN ExamVersion ev
              ON ev.id = COALESCE(e.publishedExamVersionId, e.draftExamVersionId)
             AND ev.deleted = false
            WHERE e.deleted = false
              AND (
                :categoryId IS NULL
                OR :categoryId = ''
                OR e.categoryId = :categoryId
              )
              AND (
                :searchValue IS NULL
                OR :searchValue = ''
                OR LOWER(ev.name) LIKE LOWER(CONCAT('%', :searchValue, '%'))
                OR LOWER(COALESCE(ev.description, '')) LIKE LOWER(CONCAT('%', :searchValue, '%'))
              )
            """)
    Page<ExamListRow> findExamList(@Param("searchValue") String searchValue,
                                   @Param("categoryId") String categoryId,
                                   Pageable pageable);
}
