package pl.co.auth.service.impl;

import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.dto.LoginRequest;
import pl.co.auth.dto.SignupRequest;
import pl.co.auth.entity.Role;
import pl.co.auth.entity.User;
import pl.co.auth.repository.RoleRepository;
import pl.co.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.auth.service.*;
import pl.co.common.security.RoleName;
import pl.co.common.security.UserStatus;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    @Override
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.E255, "Email already in use");
        }
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_USER"));
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .avatarUrl(request.getAvatarUrl())
                .address(request.getAddress())
                .status(UserStatus.ACTIVE.name())
                .emailVerified(false)
                .build();
        user.getRoles().add(userRole);
        User saved = userRepository.save(user);
        // generate verification token (for demo return via response header)
        emailVerificationService.createToken(saved);
        return jwtTokenService.issueExternalTokens(saved);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(ErrorCode.E238));
        if (UserStatus.BLOCKED.name().equals(user.getStatus())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.E239);
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(ErrorCode.E233);
        }
        return jwtTokenService.issueExternalTokens(user);
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        return jwtTokenService.refreshTokens(refreshToken);
    }

    @Override
    public String requestEmailVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.E238));
        return emailVerificationService.createToken(user);
    }

    @Override
    public TokenResponse verifyEmail(String token) {
        User user = emailVerificationService.verify(token);
        return jwtTokenService.issueExternalTokens(user);
    }
}

