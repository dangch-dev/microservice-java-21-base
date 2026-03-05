package pl.co.auth.service.impl;

import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.dto.LoginRequest;
import pl.co.auth.dto.SignupRequest;
import pl.co.auth.dto.GuestSignupRequest;
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

import java.util.UUID;

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
        User existing = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existing != null) {
            boolean isGuest = existing.getRoles() != null && existing.getRoles().stream()
                    .anyMatch(role -> role != null && RoleName.ROLE_GUEST.name().equals(role.getName()));
            if (!isGuest) {
                throw new ApiException(ErrorCode.E255, "Email already in use");
            }
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                    .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_USER"));
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
            existing.setFullName(request.getFullName());
            existing.setPhoneNumber(request.getPhoneNumber());
            existing.setAvatarUrl(request.getAvatarUrl());
            existing.setAddress(request.getAddress());
            existing.setStatus(UserStatus.ACTIVE.name());
            existing.setEmailVerified(false);
            existing.getRoles().add(userRole);
            User upgraded = userRepository.save(existing);

            emailVerificationService.sendOtp(upgraded);
            return jwtTokenService.issueExternalTokens(upgraded);
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

        emailVerificationService.sendOtp(saved);

        return jwtTokenService.issueExternalTokens(saved);
    }

    @Transactional
    @Override
    public TokenResponse issueGuestToken(GuestSignupRequest request) {
        User existing = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existing != null) {
            boolean isGuest = existing.getRoles() != null && existing.getRoles().stream()
                    .anyMatch(role -> role != null && RoleName.ROLE_GUEST.name().equals(role.getName()));
            if (isGuest) {
                return jwtTokenService.issueExternalTokens(existing);
            }
            throw new ApiException(ErrorCode.E255, "Email already in use");
        }
        Role guestRole = roleRepository.findByName(RoleName.ROLE_GUEST.name())
                .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_GUEST"));
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .status(UserStatus.ACTIVE.name())
                .emailVerified(true)
                .build();
        user.getRoles().add(guestRole);
        User saved = userRepository.save(user);

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
        return jwtTokenService.issueExternalTokens(user);
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        return jwtTokenService.refreshTokens(refreshToken);
    }

}

