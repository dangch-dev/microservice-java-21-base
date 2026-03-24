package pl.co.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import pl.co.common.anotation.Trim;

@Getter
@Setter
public class ExamCreateRequest {
    @NotBlank
    @Trim
    private String categoryId;

    @NotBlank
    @Trim
    private String name;

    @Trim
    private String description;

    private Integer durationMinutes;
}
