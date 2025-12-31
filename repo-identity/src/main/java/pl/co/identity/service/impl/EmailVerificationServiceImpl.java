package pl.co.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.identity.entity.EmailVerificationToken;
import pl.co.identity.entity.User;
import pl.co.identity.repository.EmailVerificationTokenRepository;
import pl.co.identity.repository.UserRepository;
import pl.co.identity.service.EmailVerificationService;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    private static final Duration VERIFY_TTL = Duration.ofHours(1);

    @Transactional
    @Override
    public String createToken(User user) {
        tokenRepository.deleteByUserId(user.getId());
        String tokenValue = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(VERIFY_TTL);
        long ttlSeconds = Math.max(1, Duration.between(Instant.now(), expiresAt).getSeconds());
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(UUID.randomUUID().toString())
                .token(tokenValue)
                .userId(user.getId())
                .expiresAt(expiresAt)
                .used(false)
                .ttlSeconds(ttlSeconds)
                .build();
        tokenRepository.save(token);
        return tokenValue; // TODO: send email
    }

    @Transactional
    @Override
    public User verify(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ApiException(ErrorCode.E241, "Verification token invalid"));
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.E241, "Verification token invalid");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.E238, "User not found"));
        user.setEmailVerified(true);
        token.setUsed(true);
        tokenRepository.save(token);
        return userRepository.save(user);
    }
}
