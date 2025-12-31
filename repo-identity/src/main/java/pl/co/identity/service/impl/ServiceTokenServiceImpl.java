package pl.co.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.security.JwtUtils;
import pl.co.identity.dto.ServiceTokenResponse;
import pl.co.identity.entity.ServiceAccount;
import pl.co.identity.repository.ServiceAccountRepository;
import pl.co.identity.service.ServiceTokenService;

import java.security.interfaces.RSAPrivateKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceTokenServiceImpl implements ServiceTokenService {

    private static final String GRANT_TYPE = "client_credentials";

    private final ServiceAccountRepository serviceAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final RSAPrivateKey jwtPrivateKey;

    @Value("${security.internal-jwt.issuer:repo-identity}")
    private String issuer;
    @Value("${security.internal-jwt.ttl:PT10M}")
    private Duration ttl;
    @Value("${security.internal-jwt.scope:internal}")
    private String defaultScope;
    @Value("${security.internal-jwt.token-type:SERVICE}")
    private String tokenType;
    @Value("${security.internal-jwt.audience:internal}")
    private String audience;

    @Override
    public ServiceTokenResponse issueToken(String grantType, String clientId, String clientSecret) {
        if (!GRANT_TYPE.equalsIgnoreCase(grantType)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Unsupported grant_type");
        }
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Missing client credentials");
        }

        ServiceAccount account = serviceAccountRepository.findByClientIdAndEnabledTrueAndDeletedFalse(clientId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Invalid client credentials"));
        if (!passwordEncoder.matches(clientSecret, account.getClientSecretHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid client credentials");
        }

        Set<String> scopes = resolveScopes(account.getScopes());
        if (scopes.isEmpty()) {
            scopes = resolveScopes(defaultScope);
        }
        List<String> audienceList = resolveList(audience);
        String token = JwtUtils.generateServiceToken(account.getServiceName(), scopes, ttl, issuer, jwtPrivateKey, audienceList, tokenType);

        return new ServiceTokenResponse(token, tokenType, ttl.toSeconds(), List.copyOf(scopes), audienceList);
    }

    private Set<String> resolveScopes(String scopeValue) {
        if (!StringUtils.hasText(scopeValue)) {
            return Set.of();
        }
        return Arrays.stream(scopeValue.split("[,\\s]+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private List<String> resolveList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split("[,\\s]+"))
                .filter(StringUtils::hasText)
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .toList();
    }
}
