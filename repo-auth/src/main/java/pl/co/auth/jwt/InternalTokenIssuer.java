package pl.co.auth.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.co.auth.dto.TokenResponse;
import pl.co.common.jwt.JwtSigner;
import pl.co.common.security.SecurityConstants;

import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InternalTokenIssuer {

    private static final List<String> AUDIENCE = List.of(SecurityConstants.AUD_INTERNAL);

    private final RSAPrivateKey jwtPrivateKey;

    @Value("${security.internal-jwt.ttl}")
    private Duration ttl;

    public TokenResponse issue(String serviceName) {
        Instant now = Instant.now();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(serviceName)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(ttl)))
                .jwtID(UUID.randomUUID().toString());
        builder.audience(AUDIENCE);
        String token = JwtSigner.sign(builder.build(), jwtPrivateKey);

        return TokenResponse.builder()
                .accessToken(token)
                .acssessExpireIn(ttl.toSeconds())
                .build();
    }
}
