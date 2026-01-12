package pl.co.auth.service;

import pl.co.auth.dto.TokenResponse;

public interface OAuthService {
    TokenResponse issueInternalToken(String clientId, String clientSecret);
}
