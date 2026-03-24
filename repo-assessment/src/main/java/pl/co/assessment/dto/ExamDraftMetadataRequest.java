package pl.co.assessment.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pl.co.common.anotation.Trim;

@Getter
@Setter
public class ExamDraftMetadataRequest {
    @NotBlank
    @Trim
    private String name;
    @Trim
    private String description;
    private Integer durationMinutes;
    @NotNull
    private Boolean shuffleQuestions;
    @NotNull
    private Boolean shuffleOptions;
}
