package pl.co.identity.service.impl;

import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.identity.dto.AuthResponse;
import pl.co.identity.dto.LoginRequest;
import pl.co.identity.dto.SignupRequest;
import pl.co.identity.entity.Role;
import pl.co.identity.entity.User;
import pl.co.identity.entity.UserStatus;
import pl.co.identity.repository.RoleRepository;
import pl.co.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.identity.service.*;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    @Override
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.E255, "Email already in use");
        }
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_USER"));
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .avatarUrl(request.getAvatarUrl())
                .address(request.getAddress())
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
        user.getRoles().add(userRole);
        User saved = userRepository.save(user);
        // generate verification token (for demo return via response header)
        emailVerificationService.createToken(saved);
        return jwtTokenService.issueTokens(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Invalid credentials"));
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.FORBIDDEN, "User is blocked");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid credentials");
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(ErrorCode.E233, "Email not verified");
        }
        return jwtTokenService.issueTokens(user);
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        return jwtTokenService.refreshTokens(refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    @Override
    public String requestPasswordReset(String email) {
        return passwordResetService.requestReset(email);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        passwordResetService.resetPassword(token, newPassword);
    }

    @Override
    public void validateResetToken(String token) {
        passwordResetService.validate(token);
    }

    @Override
    public String requestEmailVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.E238, "Username does not exist."));
        return emailVerificationService.createToken(user);
    }

    @Override
    public AuthResponse verifyEmail(String token) {
        User user = emailVerificationService.verify(token);
        return jwtTokenService.issueTokens(user);
    }
}
