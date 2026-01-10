package pl.co.common.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.jwt.record.JwtPayload;
import pl.co.common.jwt.record.JwtVerificationOptions;
import pl.co.common.security.SecurityConstants;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public final class JwtVerifier {
    private JwtVerifier() {
    }

    public static JwtPayload verify(String token, RSAPublicKey publicKey) {
        return verify(token, publicKey, null);
    }

    public static JwtPayload verify(String token, RSAPublicKey publicKey, JwtVerificationOptions options) {
        SignedJWT jwt = parse(token);
        if (!verifySignature(jwt, publicKey)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid token signature");
        }
        JWTClaimsSet claims = getClaims(jwt);
        if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
            throw new ApiException(ErrorCode.E234, "Token expired");
        }
        validateClaims(claims, options);
        return toPayload(claims);
    }

    private static JwtPayload toPayload(JWTClaimsSet claims) {
        try {
            String userId = claims.getSubject();
            @SuppressWarnings("unchecked")
            Set<String> roles = claims.getStringListClaim(SecurityConstants.CLAIM_ROLES) != null
                    ? Set.copyOf(claims.getStringListClaim(SecurityConstants.CLAIM_ROLES))
                    : Set.of();
            boolean emailVerified = Boolean.TRUE.equals(claims.getBooleanClaim(SecurityConstants.CLAIM_EMAIL_VERIFIED));
            Object type = claims.getClaim(SecurityConstants.CLAIM_TYPE);
            Object parent = claims.getClaim(SecurityConstants.CLAIM_PARENT_JTI);
            return new JwtPayload(
                    claims.getJWTID(),
                    userId,
                    roles,
                    emailVerified,
                    type == null ? null : String.valueOf(type),
                    parent == null ? null : String.valueOf(parent));
        } catch (ParseException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid token claims", e);
        }
    }

    private static void validateClaims(JWTClaimsSet claims, JwtVerificationOptions options) {
        if (options == null) {
            return;
        }
        String type = options.type();
        if (type != null && !type.isBlank()) {
            Object claimType = claims.getClaim(SecurityConstants.CLAIM_TYPE);
            if (claimType == null || !type.equalsIgnoreCase(String.valueOf(claimType))) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid token usage");
            }
        }
        List<String> expectedAudience = options.audience();
        if (expectedAudience != null && !expectedAudience.isEmpty()) {
            List<String> tokenAudience = claims.getAudience();
            boolean matched = tokenAudience != null && tokenAudience.stream().anyMatch(expectedAudience::contains);
            if (!matched) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid token audience");
            }
        }
    }

    private static boolean verifySignature(SignedJWT jwt, RSAPublicKey publicKey) {
        try {
            return jwt.verify(new RSASSAVerifier(publicKey));
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

    private static JWTClaimsSet getClaims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid token claims", e);
        }
    }
}
