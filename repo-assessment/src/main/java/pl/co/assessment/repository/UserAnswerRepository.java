package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.assessment.entity.UserAnswer;

import java.util.List;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, String> {
    List<UserAnswer> findByAttemptIdAndDeletedFalse(String attemptId);

    List<UserAnswer> findByAttemptIdAndExamVersionQuestionIdInAndDeletedFalse(String attemptId, List<String> examVersionQuestionIds);
}
