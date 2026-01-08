package pl.co.auth.service.impl;

import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.auth.entity.RefreshToken;
import pl.co.auth.repository.RefreshTokenRepository;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.auth.service.RefreshTokenService;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository repository;

    @Transactional
    @Override
    public RefreshToken store(String token, String userId, String parentJti, Duration ttl) {
        ParsedRefresh parsed = parse(token);
        long ttlSeconds = Math.max(1, Duration.between(Instant.now(), parsed.expiresAt()).getSeconds());
        RefreshToken refresh = RefreshToken.builder()
                .id(parsed.jti())
                .userId(userId)
                .token(token)
                .parentJti(parentJti)
                .expiresAt(parsed.expiresAt())
                .revoked(false)
                .ttlSeconds(ttlSeconds)
                .build();
        return repository.save(refresh);
    }

    @Override
    public RefreshToken validate(String token) {
        RefreshToken stored = repository.findByToken(token)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token not found"));
        if (stored.isRevoked()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token revoked");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token expired");
        }
        if (stored.getParentJti() != null) {
            repository.findById(stored.getParentJti()).ifPresent(parent -> {
                if (!parent.isRevoked()) {
                    throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid refresh token chain");
                }
            });
        }
        return stored;
    }

    @Transactional
    @Override
    public void revokeToken(String token) {
        repository.findByToken(token).ifPresent(refresh -> {
            refresh.setRevoked(true);
            repository.save(refresh);
        });
    }

    @Transactional
    @Override
    public void revokeById(String jti) {
        repository.findById(jti).ifPresent(refresh -> {
            refresh.setRevoked(true);
            repository.save(refresh);
        });
    }

    private ParsedRefresh parse(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            return new ParsedRefresh(
                    claims.getJWTID(),
                    claims.getExpirationTime().toInstant());
        } catch (ParseException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid refresh token", e);
        }
    }

    private record ParsedRefresh(String jti, Instant expiresAt) {
    }
}

