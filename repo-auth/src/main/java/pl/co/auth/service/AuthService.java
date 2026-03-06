package pl.co.auth.service;

import pl.co.auth.dto.TokenResponse;
import pl.co.auth.dto.LoginRequest;
import pl.co.auth.dto.SignupRequest;
import pl.co.auth.dto.GuestSignupRequest;

public interface AuthService {
    TokenResponse signup(SignupRequest request);
    TokenResponse issueGuestToken(GuestSignupRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(String refreshToken);
}
