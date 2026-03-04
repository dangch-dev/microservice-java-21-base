package pl.co.auth.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import pl.co.auth.entity.GoogleOAuthToken;
import pl.co.auth.repository.GoogleOAuthTokenRepository;
import pl.co.auth.service.GoogleOAuthTokenService;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleOAuthTokenServiceImpl implements GoogleOAuthTokenService {

    private static final String GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String REG_GOOGLE_CONNECT = "google-connect";
    private static final String REG_GOOGLE = "google";
    private static final long EXPIRY_SAFETY_WINDOW_SECONDS = 30L;

    private final GoogleOAuthTokenRepository repository;
    private final OAuth2ClientProperties oAuth2ClientProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public GoogleOAuthToken store(String userId,
                                  String googleSubject,
                                  String googleEmail,
                                  String googleName,
                                  String googleAvatarUrl,
                                  OAuth2AccessToken accessToken,
                                  OAuth2RefreshToken refreshToken) {
        if (accessToken == null) {
            return null;
        }
        GoogleOAuthToken token = repository.findFirstByUserId(userId).orElse(null);
        if (token == null) {
            token = new GoogleOAuthToken();
            token.setId(java.util.UUID.randomUUID().toString());
            token.setUserId(userId);
        }
        token.setGoogleSubject(googleSubject);
        token.setGoogleEmail(googleEmail);
        token.setGoogleName(googleName);
        token.setGoogleAvatarUrl(googleAvatarUrl);
        token.setAccessToken(accessToken.getTokenValue());
        token.setRefreshToken(refreshToken != null ? refreshToken.getTokenValue() : null);
        token.setExpiresAt(resolveExpiresAt(accessToken));
        return repository.save(token);
    }

    @Override
    public Optional<GoogleOAuthToken> findByUserId(String userId) {
        return repository.findFirstByUserId(userId);
    }

    @Override
    public void deleteByUserId(String userId) {
        repository.deleteByUserId(userId);
    }

    private Instant resolveExpiresAt(OAuth2AccessToken accessToken) {
        return accessToken.getExpiresAt();
    }

    @Override
    public Optional<GoogleOAuthToken> refreshAccessToken(GoogleOAuthToken token) {
        if (token == null || !canRefresh(token)) {
            return Optional.empty();
        }
        OAuth2ClientProperties.Registration registration = resolveRegistration();
        if (registration == null) {
            return Optional.empty();
        }
        GoogleTokenResponse response = requestTokenRefresh(registration, token.getRefreshToken());
        if (response == null || response.accessToken == null || response.accessToken.isBlank()) {
            return Optional.empty();
        }
        token.setAccessToken(response.accessToken);
        if (response.refreshToken != null && !response.refreshToken.isBlank()) {
            token.setRefreshToken(response.refreshToken);
        }
        if (response.expiresIn != null && response.expiresIn > 0) {
            token.setExpiresAt(Instant.now().plusSeconds(response.expiresIn));
        } else {
            token.setExpiresAt(null);
        }
        return Optional.of(repository.save(token));
    }

    private boolean canRefresh(GoogleOAuthToken token) {
        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            return false;
        }
        Instant expiresAt = token.getExpiresAt();
        if (expiresAt == null) {
            return true;
        }
        Instant now = Instant.now().plusSeconds(EXPIRY_SAFETY_WINDOW_SECONDS);
        return !expiresAt.isAfter(now);
    }

    private OAuth2ClientProperties.Registration resolveRegistration() {
        OAuth2ClientProperties.Registration connect = oAuth2ClientProperties.getRegistration().get(REG_GOOGLE_CONNECT);
        if (connect != null) {
            return connect;
        }
        return oAuth2ClientProperties.getRegistration().get(REG_GOOGLE);
    }

    private GoogleTokenResponse requestTokenRefresh(OAuth2ClientProperties.Registration registration,
                                                    String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", registration.getClientId());
        form.add("client_secret", registration.getClientSecret());
        form.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        try {
            return restTemplate.postForObject(GOOGLE_TOKEN_ENDPOINT, request, GoogleTokenResponse.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private static class GoogleTokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private Long expiresIn;

        @JsonProperty("refresh_token")
        private String refreshToken;
    }
}
