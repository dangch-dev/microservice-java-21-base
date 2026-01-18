package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.assessment.entity.Question;

public interface QuestionRepository extends JpaRepository<Question, String> {
}
