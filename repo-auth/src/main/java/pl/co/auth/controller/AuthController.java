package pl.co.auth.controller;

import pl.co.common.dto.ApiResponse;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.dto.LoginRequest;
import pl.co.auth.dto.RefreshTokenRequest;
import pl.co.auth.dto.SignupRequest;
import pl.co.auth.dto.ForgotPasswordRequest;
import pl.co.auth.dto.ResetPasswordRequest;
import pl.co.auth.dto.VerifyEmailRequest;
import pl.co.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.ok(null);
    }

    // Forgot password
    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = authService.requestPasswordReset(request.getEmail());
        return ApiResponse.ok(token);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.ok(null);
    }

    @GetMapping("/reset-password/validate")
    public ApiResponse<Void> validateResetToken(@RequestParam("token") String token) {
        authService.validateResetToken(token);
        return ApiResponse.ok(null);
    }

    // Verify Email
    @PostMapping("/verify-email/request")
    public ApiResponse<String> requestVerifyEmail(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = authService.requestEmailVerification(request.getEmail());
        return ApiResponse.ok(token);
    }

    @PostMapping("/verify-email")
    public ApiResponse<TokenResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.ok(authService.verifyEmail(request.getToken()));
    }
}

