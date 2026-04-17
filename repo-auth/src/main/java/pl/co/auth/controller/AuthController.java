package pl.co.auth.controller;

import pl.co.auth.service.RefreshTokenService;
import pl.co.common.dto.ApiResponse;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.dto.LoginRequest;
import pl.co.auth.dto.SigninResponse;
import pl.co.auth.dto.SigninResult;
import pl.co.auth.dto.RefreshTokenRequest;
import pl.co.auth.dto.SignupRequest;
import pl.co.auth.dto.GuestSignupRequest;
import pl.co.auth.dto.GuestLoginByCodeRequest;
import pl.co.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.co.auth.oauth.AuthCookieService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;

    @PostMapping("/signup")
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request,
                                    HttpServletResponse response) {
        TokenResponse tokens = authService.signup(request);
        authCookieService.setTokens(response, tokens);
        return ApiResponse.ok(null);
    }

    @PostMapping("/guest")
    public ApiResponse<Void> guest(@Valid @RequestBody GuestSignupRequest request,
                                   HttpServletResponse response) {
        TokenResponse tokens = authService.issueGuestToken(request);
        authCookieService.setTokens(response, tokens);
        return ApiResponse.ok(null);
    }

    @PostMapping("/guest/login-by-code")
    public ApiResponse<Void> loginByExamCode(@Valid @RequestBody GuestLoginByCodeRequest request,
                                             HttpServletResponse response) {
        TokenResponse tokens = authService.loginByExamCode(request);
        authCookieService.setTokens(response, tokens);
        return ApiResponse.ok(null);
    }

    @PostMapping("/signin")
    public ApiResponse<SigninResponse> signin(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        SigninResult result = authService.login(request);
        authCookieService.setTokens(response, result.getTokens());
        return ApiResponse.ok(SigninResponse.builder()
                .emailVerified(result.isEmailVerified())
                .build());
    }

    @PostMapping("/refresh")
    public ApiResponse<Void> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                     HttpServletRequest httpServletRequest,
                                     HttpServletResponse response) {
        String refreshToken = authCookieService.resolveRefreshToken(httpServletRequest, request);
        TokenResponse tokens = authService.refresh(refreshToken);
        authCookieService.setTokens(response, tokens);
        return ApiResponse.ok(null);
    }

    @PostMapping("/signout")
    public ApiResponse<Void> signout(@RequestBody(required = false) RefreshTokenRequest request,
                                     HttpServletRequest httpServletRequest,
                                     HttpServletResponse response) {
        String refreshToken = request != null ? request.getRefreshToken() : null;
        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = authCookieService.getRefreshTokenIfPresent(httpServletRequest);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeToken(refreshToken);
        }
        authCookieService.clearTokens(response);
        return ApiResponse.ok(null);
    }
}
