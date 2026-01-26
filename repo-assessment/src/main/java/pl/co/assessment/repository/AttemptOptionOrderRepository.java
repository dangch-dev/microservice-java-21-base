package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.assessment.entity.AttemptOptionOrder;

import java.util.List;

public interface AttemptOptionOrderRepository extends JpaRepository<AttemptOptionOrder, String> {
    List<AttemptOptionOrder> findByAttemptIdAndDeletedFalseOrderByQuestionVersionIdAscDisplayOrderAsc(String attemptId);
}
