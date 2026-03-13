package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.co.assessment.entity.ExamSession;
import pl.co.assessment.entity.ExamSessionTargetType;

import java.time.Instant;
import java.util.Optional;

public interface ExamSessionRepository extends JpaRepository<ExamSession, String> {

    Optional<ExamSession> findByIdAndDeletedFalse(String id);

    Optional<ExamSession> findByIdAndTargetTypeAndDeletedFalse(String id, ExamSessionTargetType targetType);

    @Query(value = """
            SELECT s
            FROM ExamSession s
            WHERE s.deleted = false
              AND (
                :examId IS NULL
                OR :examId = ''
                OR s.examId = :examId
              )
              AND (
                :startTime IS NULL
                OR s.startAt >= :startTime
              )
              AND (
                :endTime IS NULL
                OR s.startAt <= :endTime
              )
              AND (
                :searchValue IS NULL
                OR :searchValue = ''
                OR LOWER(s.title) LIKE LOWER(CONCAT('%', :searchValue, '%'))
                OR LOWER(s.code) LIKE LOWER(CONCAT('%', :searchValue, '%'))
              )
            ORDER BY s.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(s)
            FROM ExamSession s
            WHERE s.deleted = false
              AND (
                :examId IS NULL
                OR :examId = ''
                OR s.examId = :examId
              )
              AND (
                :startTime IS NULL
                OR s.startAt >= :startTime
              )
              AND (
                :endTime IS NULL
                OR s.startAt <= :endTime
              )
              AND (
                :searchValue IS NULL
                OR :searchValue = ''
                OR LOWER(s.title) LIKE LOWER(CONCAT('%', :searchValue, '%'))
                OR LOWER(s.code) LIKE LOWER(CONCAT('%', :searchValue, '%'))
              )
            """)
    Page<ExamSession> findManagementSessions(@Param("examId") String examId,
                                             @Param("startTime") Instant startTime,
                                             @Param("endTime") Instant endTime,
                                             @Param("searchValue") String searchValue,
                                             Pageable pageable);

    boolean existsByCodeAndDeletedFalse(String code);
}
