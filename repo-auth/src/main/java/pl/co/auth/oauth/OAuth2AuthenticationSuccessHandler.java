package pl.co.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.entity.OAuthCallbackState;
import pl.co.auth.service.OAuthLoginService;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend.base-url:}")
    private String frontendBaseUrl;

    private final OAuthLoginService oAuthLoginService;
    private final ObjectMapper objectMapper;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final AuthCookieService authCookieService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuthCallbackState callbackState = authorizationRequestRepository.getCallbackState(request);
        String targetUrl = resolveTargetUrl(callbackState);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            writeError(response, ErrorCode.UNAUTHORIZED, "Invalid oauth2 authentication");
            return;
        }
        if (!(token.getPrincipal() instanceof OidcUser oidcUser)) {
            writeError(response, ErrorCode.UNAUTHORIZED, "Invalid oauth2 principal");
            return;
        }
        if (!"google".equals(token.getAuthorizedClientRegistrationId())) {
            writeError(response, ErrorCode.UNAUTHORIZED, "Unsupported provider");
            return;
        }

        try {
            boolean connectRequested = callbackState != null
                    && "connect".equalsIgnoreCase(callbackState.getMode());
            boolean hasUserId = callbackState != null && StringUtils.hasText(callbackState.getUserId());

            if (connectRequested && !hasUserId) {
                if (StringUtils.hasText(targetUrl)) {
                    response.sendRedirect(appendError(targetUrl, "connect_requires_login"));
                    return;
                }
                writeError(response, ErrorCode.UNAUTHORIZED, "Connect requires login");
                return;
            }

            if (!connectRequested) {
                TokenResponse tokens = oAuthLoginService.loginWithGoogle(oidcUser);
                authCookieService.setTokens(response, tokens);
            }
            if (StringUtils.hasText(targetUrl)) {
                response.sendRedirect(targetUrl);
                return;
            }
            ApiResponse<Void> body = ApiResponse.ok(null);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), body);
        } catch (ApiException ex) {
            ErrorCode code = ex.getErrorCode() == null ? ErrorCode.INTERNAL_ERROR : ex.getErrorCode();
            ApiResponse<Object> body = ApiResponse.error(code.code(), ex.getMessage(), ex.getData());
            response.setStatus(code.status().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), body);
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        ApiResponse<Void> body = ApiResponse.error(code.code(), message);
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private String resolveTargetUrl(OAuthCallbackState callbackState) {
        if (callbackState != null && StringUtils.hasText(callbackState.getCallback())) {
            String resolved = buildRedirectUrl(callbackState.getCallback());
            if (StringUtils.hasText(resolved)) {
                return resolved;
            }
        }
        if (StringUtils.hasText(frontendBaseUrl)) {
            return frontendBaseUrl;
        }
        return null;
    }

    private String appendError(String targetUrl, String code) {
        String separator = targetUrl.contains("?") ? "&" : "?";
        return targetUrl + separator + "oauth_error=" + code;
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
}
