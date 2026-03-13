package pl.co.auth.service.impl;

import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.auth.dto.LoginRequest;
import pl.co.auth.dto.SignupRequest;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.dto.GuestLoginByCodeRequest;
import pl.co.auth.dto.GuestSignupRequest;
import pl.co.auth.dto.InternalGuestRequest;
import pl.co.auth.dto.InternalGuestResponse;
import pl.co.auth.dto.VerifyGuestCodeResponse;
import pl.co.auth.entity.Role;
import pl.co.auth.entity.User;
import pl.co.auth.repository.RoleRepository;
import pl.co.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import pl.co.auth.service.*;
import pl.co.common.security.RoleName;
import pl.co.common.security.UserStatus;
import pl.co.common.http.InternalApiClient;
import pl.co.common.dto.ApiResponse;

import java.util.UUID;
import pl.co.common.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final EmailVerificationService emailVerificationService;
    private final InternalApiClient internalApiClient;

    @Value("${internal.service.assessment-service}")
    private String assessmentServiceId;

    private static final String VERIFY_GUEST_CODE_PATH = "/internal/exam-sessions/verify-guest-code";

    @Transactional
    @Override
    public TokenResponse signup(SignupRequest request) {
        User existing = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existing != null) {
            if (!isGuestOnly(existing)) {
                throw new ApiException(ErrorCode.E255, "Email already in use");
            }
            Role userRole = roleRepository.findByName(RoleName.ROLE_MEMBER.name())
                    .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_MEMBER"));
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
        Role userRole = roleRepository.findByName(RoleName.ROLE_MEMBER.name())
                .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_MEMBER"));
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
            if (isGuestOnly(existing)) {
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

    @Transactional(readOnly = true)
    @Override
    public TokenResponse loginByExamCode(GuestLoginByCodeRequest request) {
        if (request == null || !StringUtils.hasText(request.getCode())) {
            throw new ApiException(ErrorCode.E221, "code is required");
        }
        ApiResponse<VerifyGuestCodeResponse> body;
        try {
            body = internalApiClient.send(
                    assessmentServiceId,
                    VERIFY_GUEST_CODE_PATH,
                    HttpMethod.POST,
                    MediaType.APPLICATION_JSON,
                    null,
                    null,
                    request,
                    new ParameterizedTypeReference<ApiResponse<VerifyGuestCodeResponse>>() {
                    },
                    false
            ).getBody();
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ApiException(ErrorCode.E227, "Code not found");
            }
            if (ex.getStatusCode().value() == 400) {
                throw new ApiException(ErrorCode.E221, "Invalid data");
            }
            throw ex;
        }
        if (body == null) {
            throw new ApiException(ErrorCode.E227, "Code not found");
        }
        if (!body.success()) {
            throw new ApiException(ErrorCode.valueOf(body.errorCode()), body.errorMessage());
        }
        VerifyGuestCodeResponse data = body.data();
        if (data == null || !data.isValid() || !StringUtils.hasText(data.getUserId())) {
            throw new ApiException(ErrorCode.E227, "Code not found");
        }
        User user = userRepository.findById(data.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.E227, "User not found"));
        if (!isGuestOnly(user)) {
            throw new ApiException(ErrorCode.E230, "No authority");
        }
        return jwtTokenService.issueExternalTokens(user);
    }

    @Transactional
    @Override
    public InternalGuestResponse upsertGuest(InternalGuestRequest request) {
        User existing = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existing != null) {
            if (!isGuestOnly(existing)) {
                throw new ApiException(ErrorCode.E255, "Email already in use");
            }

            // Update guest info
            String normalizedFullName = StringUtils.normalize(request.getFullName());
            if (normalizedFullName != null) {
                existing.setFullName(normalizedFullName);
            }
            String normalizedPhone = StringUtils.normalize(request.getPhoneNumber());
            if (normalizedPhone != null) {
                existing.setPhoneNumber(normalizedPhone);
            }
            User updated = userRepository.save(existing);
            return new InternalGuestResponse(updated.getId());
        }
        Role guestRole = roleRepository.findByName(RoleName.ROLE_GUEST.name())
                .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_GUEST"));
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(StringUtils.normalize(request.getFullName()))
                .phoneNumber(StringUtils.normalize(request.getPhoneNumber()))
                .status(UserStatus.ACTIVE.name())
                .emailVerified(true)
                .build();
        user.getRoles().add(guestRole);
        User saved = userRepository.save(user);
        return new InternalGuestResponse(saved.getId());
    }

    private boolean isGuestOnly(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        long guestCount = user.getRoles().stream()
                .filter(role -> role != null && RoleName.ROLE_GUEST.name().equals(role.getName()))
                .count();
        return guestCount == 1 && user.getRoles().size() == 1;
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

