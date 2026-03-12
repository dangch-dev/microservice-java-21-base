package pl.co.assessment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InternalGuestApiResponse {
    private boolean success;
    private String errorCode;
    private String errorMessage;
    private InternalGuestResponse data;
}
