package pl.co.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class ExamStatusUpdateRequest {
    @NotNull
    private Boolean enabled;
}
