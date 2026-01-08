package pl.co.auth.service;

import pl.co.auth.dto.TokenResponse;
import pl.co.auth.entity.User;

public interface JwtTokenService {
    TokenResponse issueExternalTokens(User user);
    TokenResponse refreshTokens(String refreshToken);
    TokenResponse issueInternalToken(String clientId, String clientSecret);
}
