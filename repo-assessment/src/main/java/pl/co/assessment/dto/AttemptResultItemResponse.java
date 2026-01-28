package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;
import pl.co.assessment.entity.json.AnswerJson;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;

import java.math.BigDecimal;

@Getter
@Builder
public class AttemptResultItemResponse {
    private final Integer order;
    private final String examVersionQuestionId;
    private final String questionVersionId;
    private final String type;
    private final QuestionContent questionContent;
    private final GradingRules gradingRules;
    private final AnswerJson answerJson;
    private final BigDecimal earnedPoints;
    private final String answerGradingStatus;
}
