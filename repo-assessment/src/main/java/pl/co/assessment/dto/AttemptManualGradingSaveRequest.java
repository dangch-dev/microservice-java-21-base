package pl.co.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class AttemptManualGradingSaveRequest {
    @Valid
    @NotEmpty
    private final List<AttemptManualGradingSaveItem> items;
}
