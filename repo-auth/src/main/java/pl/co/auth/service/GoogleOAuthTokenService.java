package pl.co.auth.service;

import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import pl.co.auth.entity.GoogleOAuthToken;

import java.util.Optional;

public interface GoogleOAuthTokenService {
    GoogleOAuthToken store(String userId,
                           String googleSubject,
                           String googleEmail,
                           String googleName,
                           String googleAvatarUrl,
                           OAuth2AccessToken accessToken,
                           OAuth2RefreshToken refreshToken);

    Optional<GoogleOAuthToken> findByUserId(String userId);

    void deleteByUserId(String userId);

    Optional<GoogleOAuthToken> refreshAccessToken(GoogleOAuthToken token);
}
