package pl.co.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {
    private final String accessToken;
    private final String refreshToken;
    private final long acssessExpireIn;
    private final long refreshExpireIn;
}

