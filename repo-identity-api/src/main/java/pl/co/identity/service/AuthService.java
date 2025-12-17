package pl.co.identity.service;

import pl.co.identity.dto.AuthResponse;
import pl.co.identity.dto.LoginRequest;
import pl.co.identity.dto.SignupRequest;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
    void logout(String refreshToken);
    String requestPasswordReset(String email);
    void resetPassword(String token, String newPassword);
    void validateResetToken(String token);
    String requestEmailVerification(String email);
    AuthResponse verifyEmail(String token);
}
