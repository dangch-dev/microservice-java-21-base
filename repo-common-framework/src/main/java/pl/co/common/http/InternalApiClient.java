package pl.co.common.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Generic internal API caller that works with service-name base URL
 * (Eureka/k8s DNS) and optional internal secret header.
 */
@Component
public class InternalApiClient {

    private final RestTemplate restTemplate;
    private final String basePattern;
    private final String internalHeader;
    private final String internalToken;

    public InternalApiClient(RestTemplate restTemplate,
                             @Value("${internal.api.base-pattern:http://%s}") String basePattern,
                             @Value("${internal.api.header:}") String internalHeader,
                             @Value("${internal.api.token:}") String internalToken) {
        this.restTemplate = restTemplate;
        this.basePattern = basePattern;
        this.internalHeader = internalHeader;
        this.internalToken = internalToken;
    }

    public <T> ResponseEntity<T> send(String serviceName,
                                      String path,
                                      HttpMethod method,
                                      Map<String, String> headers,
                                      Map<String, ?> queryParams,
                                      Object body,
                                      Class<T> responseType) {
        String baseUrl = String.format(basePattern, serviceName);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);
        if (!CollectionUtils.isEmpty(queryParams)) {
            queryParams.forEach(builder::queryParam);
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        if (internalHeader != null && !internalHeader.isBlank() && internalToken != null && !internalToken.isBlank()) {
            httpHeaders.set(internalHeader, internalToken);
        }
        if (!CollectionUtils.isEmpty(headers)) {
            headers.forEach(httpHeaders::set);
        }
        HttpEntity<?> entity = body == null ? new HttpEntity<>(httpHeaders) : new HttpEntity<>(body, httpHeaders);
        return restTemplate.exchange(builder.toUriString(), method, entity, responseType);
    }
}
