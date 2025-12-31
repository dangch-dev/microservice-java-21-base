package pl.co.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {
    @Email(message = "Input Parameter Error. Invalid data format. (email)")
    @NotBlank(message = "Required parameter is missing value. (email)")
    private String email;
}
