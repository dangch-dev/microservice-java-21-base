package pl.co.identity.dto;

import lombok.Builder;
import lombok.Getter;
import pl.co.common.security.UserStatus;

import java.util.Set;

@Getter
@Builder
public class UpdateProfileResponse {
    private final String id;
    private final String email;
    private final String fullName;
    private final String phoneNumber;
    private final String avatarUrl;
    private final String address;
    private final Set<String> roles;
    private final UserStatus status;
}
