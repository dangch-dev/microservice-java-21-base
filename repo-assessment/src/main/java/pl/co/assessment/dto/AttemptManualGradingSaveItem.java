package pl.co.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Getter
@Builder
@Jacksonized
public class AttemptManualGradingSaveItem {
    @NotBlank
    private final String examVersionQuestionId;
    @NotNull
    private final BigDecimal earnedPoints;
    private final String graderComment;
}
