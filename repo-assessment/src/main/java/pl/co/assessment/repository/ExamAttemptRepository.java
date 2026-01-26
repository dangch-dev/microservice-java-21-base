package pl.co.assessment.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.co.assessment.entity.ExamAttempt;

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

    List<ExamAttempt> findByStatusAndDeletedFalse(String status);

}
