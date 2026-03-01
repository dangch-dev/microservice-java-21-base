package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.assessment.entity.QuestionGroupVersion;

import java.util.List;

public interface QuestionGroupVersionRepository extends JpaRepository<QuestionGroupVersion, String> {
    List<QuestionGroupVersion> findByIdInAndDeletedFalse(List<String> ids);
}
