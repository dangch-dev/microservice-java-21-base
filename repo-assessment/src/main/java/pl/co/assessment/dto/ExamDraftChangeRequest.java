package pl.co.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.common.anotation.Trim;

@Getter
@Setter
public class ExamDraftChangeRequest {
    @NotBlank
    @Trim
    private String questionId;
    private Integer questionOrder;
    private Boolean deleted;
    private String type;
    @Valid
    private QuestionContent questionContent;
    @Valid
    private GradingRules gradingRules;
}
