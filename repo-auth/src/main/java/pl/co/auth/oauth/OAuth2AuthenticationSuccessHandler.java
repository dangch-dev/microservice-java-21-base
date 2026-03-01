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
import pl.co.auth.service.OAuthLoginService;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${app.frontend.oauth-callback-url:}")
    private String frontendCallbackUrl;

    private final OAuthLoginService oAuthLoginService;
    private final ObjectMapper objectMapper;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final AuthCookieService authCookieService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
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
            TokenResponse tokens = oAuthLoginService.loginWithGoogle(oidcUser);
            authCookieService.setTokens(response, tokens);
            if (StringUtils.hasText(frontendCallbackUrl)) {
                response.sendRedirect(frontendCallbackUrl);
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
}
