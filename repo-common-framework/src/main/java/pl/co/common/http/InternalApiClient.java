package pl.co.common.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.security.SecurityConstants;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Generic internal API caller that uses discovery/load-balancer
 * and internal JWT client-credentials for X-Internal-Token.
 * Forwards Authorization header from the current request when available.
 */
@Component
public class InternalApiClient {

    private static final String CLIENT_ID_PARAM = "client_id";
    private static final String CLIENT_SECRET_PARAM = "client_secret";
    private static final List<Duration> RETRY_DELAYS = List.of(
            Duration.ofMillis(200),
            Duration.ofMillis(500),
            Duration.ofMillis(1000)
    );

    private final RestTemplate restTemplate;
    private final String authServiceId;
    private final String tokenPath;
    private final String clientId;
    private final String clientSecret;
    private final Object tokenLock = new Object();
    private volatile CachedToken cachedToken;

    public InternalApiClient(RestTemplate restTemplate,
                             @Value("${internal.api.auth-service}") String authServiceId,
                             @Value("${internal.api.token-path}") String tokenPath,
                             @Value("${internal.api.client-id}") String clientId,
                             @Value("${internal.api.client-secret}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.authServiceId = authServiceId;
        this.tokenPath = tokenPath;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public <T> ResponseEntity<T> send(String serviceName,
                                      String path,
                                      HttpMethod method,
                                      Map<String, String> headers,
                                      Map<String, ?> queryParams,
                                      Object body,
                                      Class<T> responseType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(buildBaseUrl(serviceName))
                .path(normalizePath(path));
        if (!CollectionUtils.isEmpty(queryParams)) {
            queryParams.forEach(builder::queryParam);
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (!CollectionUtils.isEmpty(headers)) {
            headers.forEach(httpHeaders::set);
        }
        applyUserAuthorization(httpHeaders);
        httpHeaders.set(SecurityConstants.HEADER_INTERNAL_TOKEN, SecurityConstants.HEADER_BEARER_PREFIX + getAccessToken());
        HttpEntity<?> entity = body == null ? new HttpEntity<>(httpHeaders) : new HttpEntity<>(body, httpHeaders);
        return executeWithRetry(() -> restTemplate.exchange(builder.toUriString(), method, entity, responseType));
    }

    private String getAccessToken() {
        CachedToken current = cachedToken;
        if (current != null && current.isValid()) {
            return current.token();
        }
        synchronized (tokenLock) {
            current = cachedToken;
            if (current != null && current.isValid()) {
                return current.token();
            }
            CachedToken refreshed = fetchToken();
            cachedToken = refreshed;
            return refreshed.token();
        }
    }

    private CachedToken fetchToken() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new ApiException(ErrorCode.E305, "Missing internal API credentials");
        }
        String url = buildBaseUrl(authServiceId) + normalizePath(tokenPath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(CLIENT_ID_PARAM, clientId);
        form.add(CLIENT_SECRET_PARAM, clientSecret);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        ResponseEntity<ApiResponse<InternalTokenResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<>() {
                });
        ApiResponse<InternalTokenResponse> body = response.getBody();
        if (body == null || !body.success() || body.data() == null || body.data().accessToken() == null
                || body.data().accessToken().isBlank()) {
            throw new ApiException(ErrorCode.E305, "Unable to obtain internal token");
        }
        long ttlSeconds = Math.max(body.data().acssessExpireIn(), 1L);
        return new CachedToken(body.data().accessToken(), Instant.now().plusSeconds(ttlSeconds));
    }

    private <T> ResponseEntity<T> executeWithRetry(Supplier<ResponseEntity<T>> supplier) {
        for (int attempt = 1; ; attempt++) {
            try {
                return supplier.get();
            } catch (RestClientResponseException ex) {
                HttpStatus status = HttpStatus.resolve(ex.getRawStatusCode());
                if (status != null && status.is4xxClientError()) {
                    throw ex;
                }
                if (!shouldRetry(attempt)) {
                    throw ex;
                }
                sleep(delayForAttempt(attempt));
            } catch (ResourceAccessException ex) {
                if (!shouldRetry(attempt)) {
                    throw ex;
                }
                sleep(delayForAttempt(attempt));
            }
        }
    }

    private boolean shouldRetry(int attempt) {
        return attempt <= RETRY_DELAYS.size();
    }

    private Duration delayForAttempt(int attempt) {
        return RETRY_DELAYS.get(attempt - 1);
    }

    private void sleep(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildBaseUrl(String serviceId) {
        return "http://" + serviceId;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return expiresAt != null && expiresAt.isAfter(Instant.now());
        }
    }

    private record InternalTokenResponse(String accessToken, long acssessExpireIn) {
    }

    private void applyUserAuthorization(HttpHeaders headers) {
        if (headers.containsKey(SecurityConstants.HEADER_AUTHORIZATION)) {
            return;
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }
        String authorization = servletAttributes.getRequest().getHeader(SecurityConstants.HEADER_AUTHORIZATION);
        if (StringUtils.hasText(authorization)) {
            headers.set(SecurityConstants.HEADER_AUTHORIZATION, authorization);
        }
    }
}
