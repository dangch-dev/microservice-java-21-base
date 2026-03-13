package pl.co.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestLoginByCodeRequest {
    @NotBlank
    private String code;
}
