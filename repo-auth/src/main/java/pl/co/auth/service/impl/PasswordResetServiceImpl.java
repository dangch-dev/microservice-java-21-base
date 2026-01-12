package pl.co.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.auth.dto.ResetPasswordValidateResponse;
import pl.co.auth.entity.PasswordResetToken;
import pl.co.auth.entity.User;
import pl.co.auth.repository.PasswordResetTokenRepository;
import pl.co.auth.repository.UserRepository;
import pl.co.auth.service.PasswordResetService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.mail.MailMessage;
import pl.co.common.mail.MailPublisher;
import pl.co.common.template.TemplateLoader;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemplateLoader templateLoader;
    private final MailPublisher mailPublisher;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrl;

    private static final Duration RESET_TTL = Duration.ofMinutes(15);
    private static final String RESET_TEMPLATE_PATH = "classpath:templates/email/reset-password.html";

    @Transactional
    @Override
    public String requestReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.E238, "Username does not exist."));
        // invalidate previous tokens
        passwordResetTokenRepository.deleteByUserId(user.getId());

        // Create token
        String tokenValue = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(RESET_TTL);
        long ttlSeconds = Math.max(1, Duration.between(Instant.now(), expiresAt).getSeconds());
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID().toString())
                .token(tokenValue)
                .userId(user.getId())
                .expiresAt(expiresAt)
                .ttlSeconds(ttlSeconds)
                .build();
        passwordResetTokenRepository.save(token);

        // Create url
        String resetLink = buildResetLink(tokenValue);

        // Publish mail
        String body = renderResetPassword(user.getEmail(), resetLink, RESET_TTL);
        mailPublisher.publish(new MailMessage(
                user.getEmail(),
                "Reset your password",
                body,
                true
        ));
        return tokenValue;
    }

    @Transactional
    @Override
    public ResetPasswordValidateResponse validate(String tokenValue) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ApiException(ErrorCode.E241));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.E241);
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.E238));
        return ResetPasswordValidateResponse.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    @Transactional
    @Override
    public void resetPassword(String tokenValue, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ApiException(ErrorCode.E241));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.E241);
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.E238));
        user.setPassword(passwordEncoder.encode(newPassword));
        passwordResetTokenRepository.deleteByUserId(user.getId());
        userRepository.save(user);
    }

    private String buildResetLink(String tokenValue) {
        if (resetPasswordUrl == null || resetPasswordUrl.isBlank()) {
            return "";
        }
        String separator = resetPasswordUrl.contains("?") ? "&" : "?";
        String encoded = URLEncoder.encode(tokenValue, StandardCharsets.UTF_8);
        return resetPasswordUrl + separator + "token=" + encoded;
    }

    private String renderResetPassword(String email, String resetLink, Duration ttl) {
        String template = templateLoader.load(RESET_TEMPLATE_PATH);
        String minutes = String.valueOf(Math.max(1, ttl.toMinutes()));
        return template
                .replace("{{email}}", email == null ? "" : email)
                .replace("{{resetUrl}}", resetLink == null ? "" : resetLink)
                .replace("{{ttlMinutes}}", minutes);
    }
}
