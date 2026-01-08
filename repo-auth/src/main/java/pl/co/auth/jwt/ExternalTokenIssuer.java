package pl.co.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.co.common.jwt.JwtSigner;
import pl.co.common.jwt.record.JwtVerificationOptions;
import pl.co.common.security.SecurityConstants;
import com.nimbusds.jwt.JWTClaimsSet;

import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExternalTokenIssuer {

    private static final List<String> AUDIENCE = List.of(SecurityConstants.AUD_EXTERNAL);

    private final RSAPrivateKey jwtPrivateKey;

    @Value("${security.external-jwt.access-ttl:PT5M}")
    private Duration accessTtl;
    @Value("${security.external-jwt.refresh-ttl:P7D}")
    private Duration refreshTtl;

    public String issueAccessToken(String userId, String email, Set<String> roles, boolean emailVerified) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(accessTtl)))
                .jwtID(UUID.randomUUID().toString())
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .claim(SecurityConstants.CLAIM_ROLES, roles)
                .claim(SecurityConstants.CLAIM_EMAIL, email)
                .claim(SecurityConstants.CLAIM_EMAIL_VERIFIED, emailVerified)
                .claim(SecurityConstants.CLAIM_TYPE, SecurityConstants.TYP_ACCESS);
        builder.audience(AUDIENCE);
        return JwtSigner.sign(builder.build(), jwtPrivateKey);
    }

    public String issueRefreshToken(String userId, String parentJti) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(userId)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(refreshTtl)))
                .jwtID(UUID.randomUUID().toString())
                .claim(SecurityConstants.CLAIM_TYPE, SecurityConstants.TYP_REFRESH)
                .claim(SecurityConstants.CLAIM_PARENT_JTI, parentJti);
        builder.audience(AUDIENCE);
        return JwtSigner.sign(builder.build(), jwtPrivateKey);
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public Duration refreshTtl() {
        return refreshTtl;
    }

    public JwtVerificationOptions refreshVerificationOptions() {
        return new JwtVerificationOptions(AUDIENCE, SecurityConstants.TYP_REFRESH);
    }
}
