package pl.co.assessment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.assessment.entity.QuestionGroupItem;

import java.util.List;

public interface QuestionGroupItemRepository extends JpaRepository<QuestionGroupItem, String> {
    List<QuestionGroupItem> findByQuestionGroupVersionIdInAndDeletedFalseOrderByItemOrderAsc(List<String> versionIds);
}
