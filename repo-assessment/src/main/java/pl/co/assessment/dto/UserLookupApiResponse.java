package pl.co.assessment.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserLookupApiResponse {
    private boolean success;
    private String errorCode;
    private String errorMessage;
    private List<UserLookupResponse> data;
}
