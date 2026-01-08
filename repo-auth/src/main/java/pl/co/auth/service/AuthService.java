package pl.co.auth.service;

import pl.co.auth.dto.TokenResponse;
import pl.co.auth.dto.LoginRequest;
import pl.co.auth.dto.SignupRequest;

public interface AuthService {
    TokenResponse signup(SignupRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(String refreshToken);
    void logout(String refreshToken);
    String requestPasswordReset(String email);
    void resetPassword(String token, String newPassword);
    void validateResetToken(String token);
    String requestEmailVerification(String email);
    TokenResponse verifyEmail(String token);
}

