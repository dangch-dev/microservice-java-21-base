package pl.co.identity.dto;

import pl.co.identity.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class ProfileResponse {
    private final String id;
    private final String email;
    private final String fullName;
    private final String phoneNumber;
    private final String avatarUrl;
    private final String address;
    private final Set<String> roles;
    private final UserStatus status;
}
