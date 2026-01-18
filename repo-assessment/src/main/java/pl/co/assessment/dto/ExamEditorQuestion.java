package pl.co.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;

@Getter
@AllArgsConstructor
public class ExamEditorQuestion {
    private final String questionId;
    private final Integer questionOrder;
    private final String questionVersionId;
    private final String type;
    private final QuestionContent questionContent;
    private final GradingRules gradingRules;
}
