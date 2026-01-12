package pl.co.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pl.co.auth.dto.ForgotPasswordRequest;
import pl.co.auth.dto.ResetPasswordValidateResponse;
import pl.co.auth.dto.ResetPasswordRequest;
import pl.co.auth.service.PasswordResetService;
import pl.co.common.dto.ApiResponse;

@RestController
@RequestMapping("/forgot-password")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordResetService passwordResetService;

    @PostMapping
    public ApiResponse<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = passwordResetService.requestReset(request.getEmail());
        return ApiResponse.ok(token);
    }

    @PostMapping("/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.ok(null);
    }

    @GetMapping("/validate")
    public ApiResponse<ResetPasswordValidateResponse> validateResetToken(@RequestParam("token") String token) {
        return ApiResponse.ok(passwordResetService.validate(token));
    }
}
