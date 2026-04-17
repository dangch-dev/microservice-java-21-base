package pl.co.auth.service;

import pl.co.auth.dto.TokenResponse;
import pl.co.auth.dto.LoginRequest;
import pl.co.auth.dto.SigninResult;
import pl.co.auth.dto.SignupRequest;
import pl.co.auth.dto.GuestLoginByCodeRequest;
import pl.co.auth.dto.GuestSignupRequest;
import pl.co.auth.dto.InternalGuestRequest;
import pl.co.auth.dto.InternalGuestResponse;

public interface AuthService {
    TokenResponse signup(SignupRequest request);
    TokenResponse issueGuestToken(GuestSignupRequest request);
    TokenResponse loginByExamCode(GuestLoginByCodeRequest request);

    InternalGuestResponse upsertGuest(InternalGuestRequest request);
    SigninResult login(LoginRequest request);
    TokenResponse refresh(String refreshToken);
}
