package pl.co.auth.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${app.frontend.base-url:}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        OAuthCallbackState callbackState = authorizationRequestRepository.getCallbackState(request);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        if (callbackState != null && StringUtils.hasText(callbackState.getCallback())) {
            String target = OAuthRedirectUtils.buildRedirectUrl(callbackState.getCallback(), frontendBaseUrl);
            if (StringUtils.hasText(target)) {
                target = OAuthRedirectUtils.appendError(target, "access_denied");
                response.sendRedirect(target);
                return;
            }
        }
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.UNAUTHORIZED.code(), "OAuth2 authentication failed");
        response.setStatus(ErrorCode.UNAUTHORIZED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

}
