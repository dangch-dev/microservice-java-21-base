package pl.co.identity.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserLookupResponse {
    private final String userId;
    private final String fullName;
    private final String avatarUrl;
    private final String email;
    private final String roleName;
}
