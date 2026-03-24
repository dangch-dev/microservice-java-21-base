package pl.co.assessment.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import pl.co.assessment.entity.json.AnswerJson;

@Getter
@Setter
public class AttemptAnswerSaveItem {
    @NotBlank
    private String examVersionQuestionId;
    private AnswerJson answerJson;
}
