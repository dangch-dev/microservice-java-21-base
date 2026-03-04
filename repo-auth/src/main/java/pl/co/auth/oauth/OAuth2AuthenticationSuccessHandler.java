package pl.co.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.entity.OAuthCallbackState;
import pl.co.auth.service.GoogleOAuthTokenService;
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
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final GoogleOAuthTokenService googleOAuthTokenService;
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
        String registrationId = token.getAuthorizedClientRegistrationId();
        boolean isConnect = "google-connect".equalsIgnoreCase(registrationId);
        if (!isConnect && !"google".equals(registrationId)) {
            writeError(response, ErrorCode.UNAUTHORIZED, "Unsupported provider");
            return;
        }

        try {
            if (isConnect) {
                storeGoogleTokenForConnect(token, callbackState, targetUrl, response);
                return;
            }

            TokenResponse tokens = oAuthLoginService.loginWithGoogle(oidcUser);
            authCookieService.setTokens(response, tokens);
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
            String resolved = OAuthRedirectUtils.buildRedirectUrl(callbackState.getCallback(), frontendBaseUrl);
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
        return OAuthRedirectUtils.appendError(targetUrl, code);
    }

    private void storeGoogleTokenForConnect(OAuth2AuthenticationToken token,
                                            OAuthCallbackState callbackState,
                                            String targetUrl,
                                            HttpServletResponse response) throws IOException {
        String userId = callbackState != null ? callbackState.getUserId() : null;
        if (!StringUtils.hasText(userId)) {
            redirectConnectError(targetUrl, response, "connect_requires_login");
            return;
        }

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                token.getAuthorizedClientRegistrationId(), token.getName());
        if (client == null) {
            redirectConnectError(targetUrl, response, "connect_failed");
            return;
        }

        OAuth2AccessToken accessToken = client.getAccessToken();
        OAuth2RefreshToken refreshToken = client.getRefreshToken();
        if (accessToken == null) {
            redirectConnectError(targetUrl, response, "connect_failed");
            return;
        }

        OidcUser oidcUser = (OidcUser) token.getPrincipal();
        googleOAuthTokenService.store(
                userId,
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                oidcUser.getFullName(),
                oidcUser.getPicture(),
                accessToken,
                refreshToken);

        if (StringUtils.hasText(targetUrl)) {
            response.sendRedirect(targetUrl);
            return;
        }
        ApiResponse<Void> body = ApiResponse.ok(null);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    private void redirectConnectError(String targetUrl,
                                      HttpServletResponse response,
                                      String errorCode) throws IOException {
        if (StringUtils.hasText(targetUrl)) {
            response.sendRedirect(appendError(targetUrl, errorCode));
            return;
        }
        writeError(response, ErrorCode.UNAUTHORIZED, "Connect failed");
    }

}
