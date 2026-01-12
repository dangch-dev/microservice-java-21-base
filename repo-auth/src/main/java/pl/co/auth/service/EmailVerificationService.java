package pl.co.auth.service;

import pl.co.auth.dto.TokenResponse;

public interface EmailVerificationService {
    void sendOtp(String userId);
    TokenResponse verifyOtp(String userId, String otp);
}
