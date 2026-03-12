package pl.co.assessment.dto;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Setter
public class ExamSessionAssignmentRequest {
    @NotEmpty
    private List<@NotBlank String> userIds;
}
