package pl.co.common.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.Test;
import pl.co.common.exception.ApiException;
import pl.co.common.jwt.record.JwtPayload;
import pl.co.common.jwt.record.JwtVerificationOptions;
import pl.co.common.security.SecurityConstants;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtVerifierTest {

    @Test
    void verifyAcceptsMatchingAudienceAndType() {
        KeyPair keyPair = generateKeyPair();
        String token = signToken(
                keyPair,
                SecurityConstants.TYP_ACCESS,
                List.of(SecurityConstants.AUD_EXTERNAL));

        JwtPayload payload = JwtVerifier.verify(
                token,
                (RSAPublicKey) keyPair.getPublic(),
                new JwtVerificationOptions(List.of(SecurityConstants.AUD_EXTERNAL), SecurityConstants.TYP_ACCESS));

        assertEquals(SecurityConstants.TYP_ACCESS, payload.type());
    }

    @Test
    void verifyRejectsMismatchedAudience() {
        KeyPair keyPair = generateKeyPair();
        String token = signToken(
                keyPair,
                SecurityConstants.TYP_ACCESS,
                List.of(SecurityConstants.AUD_INTERNAL));

        ApiException ex = assertThrows(ApiException.class, () -> JwtVerifier.verify(
                token,
                (RSAPublicKey) keyPair.getPublic(),
                new JwtVerificationOptions(List.of(SecurityConstants.AUD_EXTERNAL), SecurityConstants.TYP_ACCESS)));

        assertTrue(ex.getMessage().contains("audience"));
    }

    @Test
    void verifyRejectsMismatchedType() {
        KeyPair keyPair = generateKeyPair();
        String token = signToken(
                keyPair,
                SecurityConstants.TYP_REFRESH,
                List.of(SecurityConstants.AUD_EXTERNAL));

        ApiException ex = assertThrows(ApiException.class, () -> JwtVerifier.verify(
                token,
                (RSAPublicKey) keyPair.getPublic(),
                new JwtVerificationOptions(List.of(SecurityConstants.AUD_EXTERNAL), SecurityConstants.TYP_ACCESS)));

        assertTrue(ex.getMessage().contains("usage"));
    }

    private static String signToken(KeyPair keyPair, String type, List<String> audience) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user-1")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .jwtID(UUID.randomUUID().toString())
                .claim(SecurityConstants.CLAIM_TYPE, type)
                .audience(audience)
                .build();
        return JwtSigner.sign(claims, (RSAPrivateKey) keyPair.getPrivate());
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }
}
