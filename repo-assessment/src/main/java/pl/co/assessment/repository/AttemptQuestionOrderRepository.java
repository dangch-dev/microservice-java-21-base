package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.assessment.entity.AttemptQuestionOrder;

import java.util.List;

public interface AttemptQuestionOrderRepository extends JpaRepository<AttemptQuestionOrder, String> {
    List<AttemptQuestionOrder> findByAttemptIdAndDeletedFalseOrderByDisplayOrderAsc(String attemptId);
}
