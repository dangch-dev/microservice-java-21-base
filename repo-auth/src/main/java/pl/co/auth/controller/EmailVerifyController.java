package pl.co.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.dto.VerifyEmailOtpRequest;
import pl.co.auth.service.EmailVerificationService;
import pl.co.auth.service.JwtTokenService;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.filter.principal.AuthPrincipal;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailVerifyController {

    private final EmailVerificationService emailVerificationService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/otp/send")
    public ApiResponse<Void> sendOtp(Authentication authentication) {
        String userId = resolveUserId(authentication);
        emailVerificationService.sendOtp(userId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/otp/verify")
    public ApiResponse<TokenResponse> verifyOtp(Authentication authentication,
                                       @Valid @RequestBody VerifyEmailOtpRequest request) {
        String userId = resolveUserId(authentication);
        return ApiResponse.ok(emailVerificationService.verifyOtp(userId, request.getOtp()));
    }

    private String resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        if (authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return principal.userId();
        }
        throw new ApiException(ErrorCode.UNAUTHORIZED);
    }
}
