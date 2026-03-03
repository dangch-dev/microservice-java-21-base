package pl.co.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ErrorCode;
import pl.co.auth.entity.OAuthCallbackState;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    @org.springframework.beans.factory.annotation.Value("${app.frontend.base-url:}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        OAuthCallbackState callbackState = authorizationRequestRepository.getCallbackState(request);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        if (callbackState != null && StringUtils.hasText(callbackState.getCallback())) {
            String target = buildRedirectUrl(callbackState.getCallback());
            if (StringUtils.hasText(target)) {
                target = appendError(target);
                response.sendRedirect(target);
                return;
            }
        }
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.UNAUTHORIZED.code(), "OAuth2 authentication failed");
        response.setStatus(ErrorCode.UNAUTHORIZED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private String appendError(String callback) {
        String separator = callback.contains("?") ? "&" : "?";
        return callback + separator + "oauth_error=access_denied";
    }

    private String buildRedirectUrl(String callback) {
        if (!StringUtils.hasText(callback)) {
            return null;
        }
        if (isAbsoluteUrl(callback)) {
            return isAuthorizedRedirectUri(callback) ? callback : null;
        }
        String baseOrigin = resolveBaseOrigin();
        if (!StringUtils.hasText(baseOrigin)) {
            return null;
        }
        String path = callback.startsWith("/") ? callback : "/" + callback;
        return baseOrigin + path;
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        if (!StringUtils.hasText(frontendBaseUrl)) {
            return false;
        }
        try {
            java.net.URI client = java.net.URI.create(uri);
            java.net.URI base = java.net.URI.create(frontendBaseUrl);
            return sameOrigin(client, base);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean sameOrigin(java.net.URI a, java.net.URI b) {
        if (a == null || b == null) {
            return false;
        }
        String schemeA = a.getScheme();
        String schemeB = b.getScheme();
        String hostA = a.getHost();
        String hostB = b.getHost();
        int portA = normalizedPort(a);
        int portB = normalizedPort(b);
        return StringUtils.hasText(schemeA) && StringUtils.hasText(schemeB)
                && StringUtils.hasText(hostA) && StringUtils.hasText(hostB)
                && schemeA.equalsIgnoreCase(schemeB)
                && hostA.equalsIgnoreCase(hostB)
                && portA == portB;
    }

    private int normalizedPort(java.net.URI uri) {
        int port = uri.getPort();
        if (port != -1) {
            return port;
        }
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return 80;
    }

    private boolean isAbsoluteUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private String resolveBaseOrigin() {
        if (!StringUtils.hasText(frontendBaseUrl)) {
            return null;
        }
        try {
            java.net.URI base = java.net.URI.create(frontendBaseUrl);
            if (!StringUtils.hasText(base.getScheme()) || !StringUtils.hasText(base.getHost())) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(base.getScheme()).append("://").append(base.getHost());
            if (base.getPort() != -1) {
                sb.append(":").append(base.getPort());
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }
}
