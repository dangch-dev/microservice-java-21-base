package pl.co.assessment.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import pl.co.assessment.entity.ExamAttempt;
import pl.co.assessment.projection.AttemptListRow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ea
            FROM ExamAttempt ea
            WHERE ea.examId = :examId
              AND ea.createdBy = :userId
              AND ea.status IN :statuses
              AND ea.deleted = false
            ORDER BY ea.startTime DESC
            """)
    List<ExamAttempt> findActiveAttemptsForUpdate(@Param("examId") String examId,
                                                  @Param("userId") String userId,
                                                  @Param("statuses") List<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT ea
            FROM ExamAttempt ea
            WHERE ea.id = :id
              AND ea.deleted = false
            """)
    Optional<ExamAttempt> findByIdAndDeletedFalseForUpdate(@Param("id") String id);

    Optional<ExamAttempt> findByIdAndDeletedFalse(String id);

    @Query("""
            SELECT ea
            FROM ExamAttempt ea
            JOIN ExamVersion ev ON ev.id = ea.examVersionId AND ev.deleted = false
            WHERE ea.status = 'IN_PROGRESS'
              AND ea.deleted = false
              AND ea.startTime IS NOT NULL
              AND ev.durationMinutes IS NOT NULL
              AND timestampadd(MINUTE, ev.durationMinutes + 1, ea.startTime) <= CURRENT_TIMESTAMP
            """)
    List<ExamAttempt> findExpiredAttempts();

    @Query(value = """
            SELECT
                ea.id AS attemptId,
                ea.examId AS examId,
                ea.examVersionId AS examVersionId,
                ev.name AS name,
                ev.description AS description,
                ev.durationMinutes AS durationMinutes,
                ea.status AS status,
                ea.gradingStatus AS gradingStatus,
                ea.startTime AS startTime,
                ea.endTime AS endTime,
                ea.score AS score,
                ea.maxScore AS maxScore,
                ea.percent AS percent
            FROM ExamAttempt ea
            JOIN ExamVersion ev
              ON ev.id = ea.examVersionId
             AND ev.deleted = false
            WHERE ea.deleted = false
              AND ea.createdBy = :userId
              AND (
                :status IS NULL
                OR :status = ''
                OR ea.status = :status
              )
              AND (
                :gradingStatus IS NULL
                OR :gradingStatus = ''
                OR ea.gradingStatus = :gradingStatus
              )
              AND (
                :fromTime IS NULL
                OR ea.startTime >= :fromTime
              )
              AND (
                :toTime IS NULL
                OR ea.startTime <= :toTime
              )
            ORDER BY ea.startTime DESC
            """,
            countQuery = """
            SELECT COUNT(ea)
            FROM ExamAttempt ea
            JOIN ExamVersion ev
              ON ev.id = ea.examVersionId
             AND ev.deleted = false
            WHERE ea.deleted = false
              AND ea.createdBy = :userId
              AND (
                :status IS NULL
                OR :status = ''
                OR ea.status = :status
              )
              AND (
                :gradingStatus IS NULL
                OR :gradingStatus = ''
                OR ea.gradingStatus = :gradingStatus
              )
              AND (
                :fromTime IS NULL
                OR ea.startTime >= :fromTime
              )
              AND (
                :toTime IS NULL
                OR ea.startTime <= :toTime
              )
            """)
    Page<AttemptListRow> findAttemptList(@Param("userId") String userId,
                                         @Param("status") String status,
                                         @Param("gradingStatus") String gradingStatus,
                                         @Param("fromTime") Instant fromTime,
                                         @Param("toTime") Instant toTime,
                                         Pageable pageable);

}
