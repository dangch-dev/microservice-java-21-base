package pl.co.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.identity.entity.PasswordResetToken;
import pl.co.identity.entity.User;
import pl.co.identity.repository.PasswordResetTokenRepository;
import pl.co.identity.repository.UserRepository;
import pl.co.identity.service.PasswordResetService;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Duration RESET_TTL = Duration.ofMinutes(15);

    @Transactional
    @Override
    public String requestReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.E238, "Username does not exist."));
        // invalidate previous tokens
        tokenRepository.deleteByUserId(user.getId());
        String tokenValue = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(RESET_TTL);
        long ttlSeconds = Math.max(1, Duration.between(Instant.now(), expiresAt).getSeconds());
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID().toString())
                .token(tokenValue)
                .userId(user.getId())
                .expiresAt(expiresAt)
                .used(false)
                .ttlSeconds(ttlSeconds)
                .build();
        tokenRepository.save(token);
        // TODO: send email with token; for now return token for testing
        return tokenValue;
    }

    @Transactional
    @Override
    public void validate(String tokenValue) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ApiException(ErrorCode.E248, "Invalid reset token"));
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.E248, "Invalid reset token");
        }
    }

    @Transactional
    @Override
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ApiException(ErrorCode.E248, "Invalid reset token"));
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.E248, "Invalid reset token");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.E238, "User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        token.setUsed(true);
        tokenRepository.save(token);
        userRepository.save(user);
    }
}
