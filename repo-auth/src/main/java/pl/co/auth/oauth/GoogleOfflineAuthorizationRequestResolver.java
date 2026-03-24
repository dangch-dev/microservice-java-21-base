package pl.co.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GoogleOfflineAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public GoogleOfflineAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return buildAuthorizationRequest(request, defaultResolver.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return buildAuthorizationRequest(request, defaultResolver.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest buildAuthorizationRequest(HttpServletRequest httpRequest,
                                                                 OAuth2AuthorizationRequest baseRequest) {
        if (httpRequest == null || baseRequest == null) {
            return null;
        }
        String registrationId = baseRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);
        if (!isGoogleRegistration(registrationId)) {
            return baseRequest;
        }

        Set<String> scopes = mergeScopes(baseRequest, httpRequest);
        Map<String, Object> parameters = buildAdditionalParameters(baseRequest, httpRequest);

        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(baseRequest)
                .additionalParameters(parameters);
        if (!scopes.isEmpty()) {
            builder.scopes(scopes);
        }
        return builder.build();
    }

    private Set<String> mergeScopes(OAuth2AuthorizationRequest baseRequest, HttpServletRequest httpRequest) {
        Set<String> scopes = new LinkedHashSet<>(baseRequest.getScopes());
        String rawScope = httpRequest.getParameter("scope");
        if (!StringUtils.hasText(rawScope)) {
            return scopes;
        }
        String[] tokens = StringUtils.tokenizeToStringArray(rawScope, " ,");
        if (tokens == null) {
            return scopes;
        }
        for (String token : tokens) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            scopes.add(token.trim());
        }
        return scopes;
    }

    private Map<String, Object> buildAdditionalParameters(OAuth2AuthorizationRequest baseRequest,
                                                          HttpServletRequest httpRequest) {
        Map<String, Object> parameters = new LinkedHashMap<>(baseRequest.getAdditionalParameters());
        parameters.put("access_type", "offline");
        parameters.put("include_granted_scopes", "true");

        if ("true".equalsIgnoreCase(httpRequest.getParameter("force_consent"))) {
            parameters.put("prompt", "consent");
        }

        return parameters;
    }

    private boolean isGoogleRegistration(String registrationId) {
        return "google".equals(registrationId) || "google-connect".equals(registrationId);
    }
}
