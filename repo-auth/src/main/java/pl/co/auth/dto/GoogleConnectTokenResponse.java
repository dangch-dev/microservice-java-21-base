package pl.co.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GoogleConnectTokenResponse {
    private final String accessToken;
    private final Instant expiresAt;
    private final String googleName;
    private final String googleAvatarUrl;
    private final String googleEmail;
}
