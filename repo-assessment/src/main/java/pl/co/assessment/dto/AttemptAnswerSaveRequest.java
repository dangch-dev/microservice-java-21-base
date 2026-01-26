package pl.co.assessment.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AttemptAnswerSaveRequest {
    @Valid
    private List<AttemptAnswerSaveItem> answers;
}
