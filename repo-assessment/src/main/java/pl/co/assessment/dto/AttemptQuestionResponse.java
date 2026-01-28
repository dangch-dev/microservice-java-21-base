package pl.co.assessment.dto;

import lombok.Builder;
import lombok.Getter;
import pl.co.assessment.entity.json.QuestionContent;

@Getter
@Builder
public class AttemptQuestionResponse {
    private final Integer order;
    private final String examVersionQuestionId;
    private final String questionVersionId;
    private final String type;
    private final QuestionContent questionContent;
}
