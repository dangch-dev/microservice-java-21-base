package pl.co.auth.service;

import pl.co.auth.entity.User;

public interface EmailVerificationService {
    String createToken(User user);
    User verify(String tokenValue);
}

