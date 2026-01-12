package pl.co.auth.service;

public interface PasswordResetService {
    String requestReset(String email);
    pl.co.auth.dto.ResetPasswordValidateResponse validate(String tokenValue);
    void resetPassword(String tokenValue, String newPassword);
}

