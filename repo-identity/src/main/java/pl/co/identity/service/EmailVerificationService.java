package pl.co.identity.service;

import pl.co.identity.entity.User;

public interface EmailVerificationService {
    String createToken(User user);
    User verify(String tokenValue);
}
