package pl.co.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import pl.co.assessment.entity.json.GradingRules;
import pl.co.assessment.entity.json.QuestionContent;
import pl.co.common.anotation.Trim;

import java.util.List;

@Getter
@Setter
public class ExamDraftChangeRequest {
    @NotBlank
    @Trim
    private String questionId;
    @NotNull
    @Positive
    private Integer questionOrder;
    @NotEmpty
    private List<ExamDraftChangeType> changeTypes;
    @NotBlank
    @Trim
    private String type;
    @Valid
    @NotNull
    private QuestionContent questionContent;
    @Valid
    @NotNull
    private GradingRules gradingRules;
}
