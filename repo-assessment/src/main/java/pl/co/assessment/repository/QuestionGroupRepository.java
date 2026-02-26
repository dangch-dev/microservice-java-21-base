package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.assessment.entity.QuestionGroup;

import java.util.Optional;

public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, String> {
    Optional<QuestionGroup> findByIdAndDeletedFalse(String id);
}
