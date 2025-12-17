package pl.co.common.security;

import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public final class JwtUtils {
    private JwtUtils() {
    }

    public static String generateAccessToken(String userId,
                                             String email,
                                             Set<String> roles,
                                             boolean emailVerified,
                                             Duration ttl,
                                             String issuer,
                                             String secret) {
        return signToken(userId, email, roles, emailVerified, ttl, issuer, secret, UUID.randomUUID().toString());
    }

    public static String generateRefreshToken(String userId,
                                              Duration ttl,
                                              String issuer,
                                              String secret,
                                              String parentJti) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId)
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .jwtID(UUID.randomUUID().toString())
                .claim("typ", "refresh")
                .claim("prt", parentJti)
                .build();
        return sign(claims, secret);
    }

    public static JwtPayload verify(String token, String secret) {
        SignedJWT jwt = parse(token);
        boolean valid = verifySignature(jwt, secret);
        if (!valid) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid token signature");
        }
        try {
            if (jwt.getJWTClaimsSet().getExpirationTime() != null &&
                    jwt.getJWTClaimsSet().getExpirationTime().before(new Date())) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "Token expired");
            }
        } catch (ParseException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid token claims", e);
        }
        return toPayload(jwt);
    }

    private static JwtPayload toPayload(SignedJWT jwt) {
        try {
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            String userId = claims.getSubject();
            String email = claims.getStringClaim("email");
            @SuppressWarnings("unchecked")
            Set<String> roles = claims.getStringListClaim(SecurityConstants.CLAIM_ROLES) != null
                    ? Set.copyOf(claims.getStringListClaim(SecurityConstants.CLAIM_ROLES))
                    : Set.of();
            boolean emailVerified = Boolean.TRUE.equals(claims.getBooleanClaim(SecurityConstants.CLAIM_EMAIL_VERIFIED));
            return new JwtPayload(
                    claims.getJWTID(),
                    userId,
                    email,
                    roles,
                    emailVerified,
                    claims.getClaim("typ"),
                    claims.getClaim("prt"));
        } catch (ParseException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid token claims", e);
        }
    }

    private static String signToken(String userId,
                                    String email,
                                    Set<String> roles,
                                    boolean emailVerified,
                                    Duration ttl,
                                    String issuer,
                                    String secret,
                                    String jti) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId)
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .jwtID(jti)
                .claim("email", email)
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .claim(SecurityConstants.CLAIM_ROLES, roles)
                .claim(SecurityConstants.CLAIM_EMAIL_VERIFIED, emailVerified)
                .claim("typ", "access")
                .build();
        return sign(claims, secret);
    }

    private static String sign(JWTClaimsSet claims, String secret) {
        try {
            byte[] secretBytes = secret.getBytes();
            if (secretBytes.length < 32) {
                throw new ApiException(ErrorCode.INTERNAL_ERROR, "JWT secret must be at least 256 bits");
            }
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(new MACSigner(secretBytes));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to sign JWT", e);
        }
    }

    private static boolean verifySignature(SignedJWT jwt, String secret) {
        try {
            return jwt.verify(new MACVerifier(secret.getBytes()));
        } catch (JOSEException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid token signature", e);
        }
    }

    private static SignedJWT parse(String token) {
        try {
            return SignedJWT.parse(token);
        } catch (ParseException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Malformed token", e);
        }
    }

    public record JwtPayload(String jti,
                             String userId,
                             String email,
                             Set<String> roles,
                             boolean emailVerified,
                             Object type,
                             Object parentJti) {
    }
}
