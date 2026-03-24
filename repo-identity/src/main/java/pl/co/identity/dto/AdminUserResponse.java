package pl.co.identity.dto;

import pl.co.common.security.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;

@Getter
@Builder
public class AdminUserResponse {
    private final String id;
    private final String email;
    private final String fullName;
    private final String phoneNumber;
    private final String avatarUrl;
    private final String address;
    private final Set<String> roles;
    private final UserStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
}
