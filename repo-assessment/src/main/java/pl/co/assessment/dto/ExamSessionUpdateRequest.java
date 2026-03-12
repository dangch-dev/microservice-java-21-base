package pl.co.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ExamSessionUpdateRequest {

    @NotBlank
    private String title;

    private Instant startAt;

    private Instant endAt;

    private String accessCode;
}
