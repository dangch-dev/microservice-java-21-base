package pl.co.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.entity.RefreshToken;
import pl.co.auth.entity.Role;
import pl.co.auth.entity.ServiceAccount;
import pl.co.auth.entity.User;
import pl.co.auth.jwt.InternalTokenIssuer;
import pl.co.auth.jwt.ExternalTokenIssuer;
import pl.co.auth.repository.ServiceAccountRepository;
import pl.co.auth.repository.UserRepository;
import pl.co.auth.service.JwtTokenService;
import pl.co.auth.service.RefreshTokenService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.jwt.JwtVerifier;
import pl.co.common.jwt.record.JwtPayload;
import pl.co.common.security.UserStatus;

import java.security.interfaces.RSAPublicKey;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final ServiceAccountRepository serviceAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RSAPublicKey jwtPublicKey;
    private final ExternalTokenIssuer externalTokenIssuer;
    private final InternalTokenIssuer internalTokenIssuer;

    @Override
    public TokenResponse issueExternalTokens(User user) {
        Set<String> roles = extractRoleNames(user);
        String userId = user.getId();
        String access = externalTokenIssuer.issueAccessToken(userId, roles, user.isEmailVerified());
        String refresh = externalTokenIssuer.issueRefreshToken(userId, null);
        refreshTokenService.store(refresh, userId, null, externalTokenIssuer.refreshTtl());
        return TokenResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresInSeconds(externalTokenIssuer.accessTtlSeconds())
                .build();
    }

    @Override
    public TokenResponse refreshTokens(String refreshToken) {
        JwtPayload payload = JwtVerifier.verify(refreshToken, jwtPublicKey, externalTokenIssuer.refreshVerificationOptions());
        RefreshToken stored = refreshTokenService.validate(refreshToken);
        if (!Objects.equals(stored.getParentJti(), payload.parentJti())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid refresh token chain");
        }

        String userId = payload.userId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.E227)); //"No data found"
        if (UserStatus.BLOCKED.name().equals(user.getStatus())) {
            throw new ApiException(ErrorCode.E249); // User is blocked
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(ErrorCode.E233); // Email not verified
        }
        Set<String> roles = extractRoleNames(user);
        String access = externalTokenIssuer.issueAccessToken(userId, roles, user.isEmailVerified());
        String rotated = externalTokenIssuer.issueRefreshToken(userId, payload.jti());

        refreshTokenService.revokeById(payload.jti());
        refreshTokenService.store(rotated, userId, payload.jti(), externalTokenIssuer.refreshTtl());

        return TokenResponse.builder()
                .accessToken(access)
                .refreshToken(rotated)
                .expiresInSeconds(externalTokenIssuer.accessTtlSeconds())
                .build();
    }

    private Set<String> extractRoleNames(User user) {
        return user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }

    @Override
    public TokenResponse issueInternalToken(String clientId, String clientSecret) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Missing client credentials");
        }

        ServiceAccount account = serviceAccountRepository.findByClientIdAndEnabledTrueAndDeletedFalse(clientId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Invalid client credentials"));
        if (!passwordEncoder.matches(clientSecret, account.getClientSecretHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid client credentials");
        }

        return internalTokenIssuer.issue(account.getServiceName());
    }
}
