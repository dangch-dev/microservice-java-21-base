package pl.co.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "Required parameter is missing value. (token)")
    private String token;

    @NotBlank(message = "Required parameter is missing value. (password)")
    @Size(min = 8, max = 100, message = "Input Parameter Error. Invalid data length. (password)")
    private String newPassword;
}

