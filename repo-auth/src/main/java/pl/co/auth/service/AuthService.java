package pl.co.auth.service;

import pl.co.auth.dto.TokenResponse;
import pl.co.auth.dto.LoginRequest;
import pl.co.auth.dto.SignupRequest;

public interface AuthService {
    TokenResponse signup(SignupRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(String refreshToken);
    String requestEmailVerification(String email);
    TokenResponse verifyEmail(String token);
}

