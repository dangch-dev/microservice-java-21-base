package pl.co.identity.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private final String accessToken;
    private final String refreshToken;
    private final long expiresInSeconds;
    private final boolean emailVerified;
}
