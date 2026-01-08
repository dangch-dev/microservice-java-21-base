package pl.co.auth.service;

public interface PasswordResetService {
    String requestReset(String email);
    void validate(String tokenValue);
    void resetPassword(String tokenValue, String newPassword);
}

