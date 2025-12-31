package pl.co.identity.service;

import pl.co.identity.dto.AuthResponse;
import pl.co.identity.entity.User;

public interface JwtTokenService {
    AuthResponse issueTokens(User user);
    AuthResponse refreshTokens(String refreshToken);
}
