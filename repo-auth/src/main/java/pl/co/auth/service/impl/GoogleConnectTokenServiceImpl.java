package pl.co.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.co.auth.dto.GoogleConnectTokenResponse;
import pl.co.auth.entity.GoogleOAuthToken;
import pl.co.auth.service.GoogleConnectTokenService;
import pl.co.auth.service.GoogleOAuthTokenService;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GoogleConnectTokenServiceImpl implements GoogleConnectTokenService {

    private final GoogleOAuthTokenService googleOAuthTokenService;

    @Override
    public GoogleConnectTokenResponse getToken(String userId) {
        GoogleOAuthToken token = googleOAuthTokenService.findByUserId(userId).orElse(null);
        if (token == null) {
            return buildResponse(null);
        }
        if (!isTokenValid(token)) {
            token = googleOAuthTokenService.refreshAccessToken(token).orElse(null);
            if (token == null || !isTokenValid(token)) {
                return buildResponse(null);
            }
        }
        return buildResponse(token);
    }

    private boolean isTokenValid(GoogleOAuthToken token) {
        if (token.getAccessToken() == null || token.getAccessToken().isBlank()) {
            return false;
        }
        Instant expiresAt = token.getExpiresAt();
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }

    private GoogleConnectTokenResponse buildResponse(GoogleOAuthToken token) {
        return GoogleConnectTokenResponse.builder()
                .accessToken(token != null ? token.getAccessToken() : null)
                .expiresAt(token != null ? token.getExpiresAt() : null)
                .googleName(token != null ? token.getGoogleName() : null)
                .googleAvatarUrl(token != null ? token.getGoogleAvatarUrl() : null)
                .googleEmail(token != null ? token.getGoogleEmail() : null)
                .build();
    }
}
