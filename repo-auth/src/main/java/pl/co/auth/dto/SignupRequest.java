package pl.co.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    @Email(message = "Input Parameter Error. Invalid data format. (email)")
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, max = 100, message = "Input Parameter Error. Invalid data length. (password)")
    private String password;

    @NotBlank
    @Size(max = 120, message = "Input Parameter Error. Invalid data length. (fullName)")
    private String fullName;

    @Size(max = 30, message = "Input Parameter Error. Invalid data length. (phoneNumber)")
    private String phoneNumber;

    @Size(max = 255, message = "Input Parameter Error. Invalid data length. (avatarUrl)")
    private String avatarUrl;

    @Size(max = 255, message = "Input Parameter Error. Invalid data length. (address)")
    private String address;
}

