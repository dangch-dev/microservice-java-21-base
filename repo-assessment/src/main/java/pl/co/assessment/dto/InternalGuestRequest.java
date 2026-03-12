package pl.co.assessment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalGuestRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
}
