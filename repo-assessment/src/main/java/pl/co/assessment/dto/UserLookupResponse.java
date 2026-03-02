package pl.co.assessment.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLookupResponse {
    private String userId;
    private String fullName;
    private String avatarUrl;
    private String email;
    private String roleName;
}
