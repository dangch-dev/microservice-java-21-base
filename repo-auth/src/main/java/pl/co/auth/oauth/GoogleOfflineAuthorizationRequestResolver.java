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
        return customize(request, defaultResolver.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return customize(request, defaultResolver.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest customize(HttpServletRequest httpRequest,
                                                 OAuth2AuthorizationRequest request) {
        if (request == null || httpRequest == null) {
            return null;
        }
        String registrationId = request.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);
        if (!"google".equals(registrationId)) {
            return request;
        }

        Set<String> scopes = new LinkedHashSet<>(request.getScopes());
        String rawScope = httpRequest.getParameter("scope");
        if (StringUtils.hasText(rawScope)) {
            String[] tokens = StringUtils.tokenizeToStringArray(rawScope, " ,");
            if (tokens != null) {
                for (String token : tokens) {
                    if (!StringUtils.hasText(token)) {
                        continue;
                    }
                    scopes.add(token.trim());
                }
            }
        }

        Map<String, Object> parameters = new LinkedHashMap<>(request.getAdditionalParameters());
        parameters.put("access_type", "offline");
        parameters.put("include_granted_scopes", "true");

        String forceConsent = httpRequest.getParameter("force_consent");
        if ("true".equalsIgnoreCase(forceConsent)) {
            parameters.put("prompt", "consent");
        }

        String mode = httpRequest.getParameter("mode");
        if (StringUtils.hasText(mode)) {
            parameters.put("mode", mode);
        }

        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(request)
                .additionalParameters(parameters);
        if (!scopes.isEmpty()) {
            builder.scopes(scopes);
        }
        return builder.build();
    }
}
