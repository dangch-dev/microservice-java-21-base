package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;
import pl.co.assessment.entity.json.AnswerJson;

@Getter
@Builder
public class AttemptAnswerResponse {
    private final String examVersionQuestionId;
    private final AnswerJson answerJson;
}
