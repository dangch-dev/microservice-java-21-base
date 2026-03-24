package pl.co.assessment.service;

import pl.co.assessment.dto.QuestionGroupResponse;
import pl.co.assessment.entity.ExamVersionQuestion;

import java.util.List;

public interface QuestionGroupService {
    List<QuestionGroupResponse> buildGroups(List<ExamVersionQuestion> mappings);
}
