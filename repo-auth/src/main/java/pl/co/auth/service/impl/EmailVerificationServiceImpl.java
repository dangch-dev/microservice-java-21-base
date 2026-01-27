package pl.co.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.service.JwtTokenService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.auth.entity.EmailVerificationOTP;
import pl.co.auth.entity.User;
import pl.co.auth.repository.EmailVerificationOTPRepository;
import pl.co.auth.repository.UserRepository;
import pl.co.auth.service.EmailVerificationService;
import pl.co.common.mail.MailMessage;
import pl.co.common.event.EventPublisher;
import pl.co.common.security.UserStatus;
import pl.co.common.template.TemplateLoader;

import java.time.Duration;
import java.time.Instant;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationOTPRepository emailVerificationOTPRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final TemplateLoader templateLoader;

    private static final Duration VERIFY_TTL = Duration.ofMinutes(10);
    private static final int OTP_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String OTP_TEMPLATE_PATH = "classpath:templates/email/verify-otp.html";
    private final JwtTokenService jwtTokenService;

    @Value("${kafka.topics.mail}")
    private String mailTopic;

    @Transactional
    @Override
    public void sendOtp(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.E238));
        sendOtp(user);
    }

    @Transactional
    @Override
    public void sendOtp(User user) {
        if (user == null) {
            throw new ApiException(ErrorCode.E238);
        }

        // Delete old OTP
        emailVerificationOTPRepository.deleteByUserId(user.getId());

        String otpValue = generateOtp();
        Instant expiresAt = Instant.now().plus(VERIFY_TTL);
        long ttlSeconds = Math.max(1, Duration.between(Instant.now(), expiresAt).getSeconds());
        String otpHash = passwordEncoder.encode(otpValue);
        EmailVerificationOTP token = EmailVerificationOTP.builder()
                .id(user.getId())
                .userId(user.getId())
                .otpHash(otpHash)
                .expiresAt(expiresAt)
                .attempts(0)
                .ttlSeconds(ttlSeconds)
                .build();
        emailVerificationOTPRepository.save(token);
        String body = renderMailVerifyOtp(user.getEmail(), otpValue, VERIFY_TTL);

        // Publish Mail
        eventPublisher.publish(mailTopic, user.getId(), new MailMessage(
                user.getEmail(),
                "Verify your account",
                body,
                true
        ));
    }

    @Transactional
    @Override
    public TokenResponse verifyOtp(String userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.E238));
        EmailVerificationOTP token = emailVerificationOTPRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.E602, ErrorCode.E602.message()));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            emailVerificationOTPRepository.deleteByUserId(user.getId());
            throw new ApiException(ErrorCode.E242, ErrorCode.E242.message());
        }
        if (token.getAttempts() >= MAX_ATTEMPTS) {
            emailVerificationOTPRepository.deleteByUserId(user.getId());
            throw new ApiException(ErrorCode.E602, ErrorCode.E602.message());
        }
        if (!passwordEncoder.matches(otp, token.getOtpHash())) {
            token.setAttempts(token.getAttempts() + 1);
            emailVerificationOTPRepository.save(token);
            throw new ApiException(ErrorCode.E602, ErrorCode.E602.message());
        }
        user.setEmailVerified(true);
        // Delete OTP
        emailVerificationOTPRepository.deleteByUserId(user.getId());

        // Update user
        userRepository.save(user);

        // issue new token
        return jwtTokenService.issueExternalTokens(user);
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int value = SECURE_RANDOM.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", value);
    }

    private String renderMailVerifyOtp(String email, String otp, Duration ttl) {
        String template = templateLoader.load(OTP_TEMPLATE_PATH);
        String minutes = String.valueOf(Math.max(1, ttl.toMinutes()));
        return template
                .replace("{{email}}", email == null ? "" : email)
                .replace("{{otp}}", otp == null ? "" : otp)
                .replace("{{ttlMinutes}}", minutes);
    }
}
