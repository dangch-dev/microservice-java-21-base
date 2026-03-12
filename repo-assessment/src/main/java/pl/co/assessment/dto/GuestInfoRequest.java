package pl.co.assessment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuestInfoRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    private String phoneNumber;
}
