package pl.co.identity.dto;

import java.util.List;

public record ServiceTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        List<String> scope,
        List<String> audience
) {
}
