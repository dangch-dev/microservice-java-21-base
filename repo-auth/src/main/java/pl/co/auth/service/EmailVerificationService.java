package pl.co.auth.service;

import pl.co.auth.dto.TokenResponse;
import pl.co.auth.entity.User;

public interface EmailVerificationService {
    void sendOtp(String userId);
    void sendOtp(User user);
    TokenResponse verifyOtp(String userId, String otp);
}
