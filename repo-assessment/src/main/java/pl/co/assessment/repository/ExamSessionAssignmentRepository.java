package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.assessment.entity.ExamSessionAssignment;

import java.util.List;
import java.util.Optional;

public interface ExamSessionAssignmentRepository extends JpaRepository<ExamSessionAssignment, String> {

    List<ExamSessionAssignment> findBySessionIdAndDeletedFalse(String sessionId);

    Optional<ExamSessionAssignment> findBySessionIdAndUserIdAndDeletedFalse(String sessionId, String userId);

    Optional<ExamSessionAssignment> findByCodeAndDeletedFalse(String code);

    boolean existsByCodeAndDeletedFalse(String code);

    long countBySessionIdAndDeletedFalse(String sessionId);
}
