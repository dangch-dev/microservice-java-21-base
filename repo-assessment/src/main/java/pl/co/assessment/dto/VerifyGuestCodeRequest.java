package pl.co.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyGuestCodeRequest {
    @NotBlank
    private String code;
}
