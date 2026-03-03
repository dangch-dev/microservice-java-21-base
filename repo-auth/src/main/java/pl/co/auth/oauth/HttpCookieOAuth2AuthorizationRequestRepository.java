package pl.co.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import pl.co.auth.entity.OAuthCallbackState;
import pl.co.auth.repository.OAuthCallbackStateRepository;
import pl.co.common.security.AuthUtils;
import org.springframework.security.core.context.SecurityContextHolder;

public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String AUTH_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    public static final String REDIRECT_URI_PARAM_NAME = "callback";
    public static final String MODE_PARAM_NAME = "mode";
    private static final int COOKIE_EXPIRE_SECONDS = 180;
    private final OAuthCallbackStateRepository callbackStateRepository;

    public HttpCookieOAuth2AuthorizationRequestRepository(OAuthCallbackStateRepository callbackStateRepository) {
        this.callbackStateRepository = callbackStateRepository;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, AUTH_REQUEST_COOKIE_NAME)
                .map(cookie -> CookieUtils.deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }
        String serialized = CookieUtils.serialize(authorizationRequest);
        CookieUtils.addCookie(response, AUTH_REQUEST_COOKIE_NAME, serialized, COOKIE_EXPIRE_SECONDS);

        String redirectUri = request.getParameter(REDIRECT_URI_PARAM_NAME);
        String mode = request.getParameter(MODE_PARAM_NAME);
        boolean isConnect = "connect".equalsIgnoreCase(mode);
        if (authorizationRequest.getState() != null && (isConnect || (redirectUri != null && !redirectUri.isBlank()))) {
            String userId = null;
            if (isConnect) {
                userId = resolveUserIdIfAuthenticated();
            }
            OAuthCallbackState state = OAuthCallbackState.builder()
                    .id(authorizationRequest.getState())
                    .callback(redirectUri)
                    .mode(mode)
                    .userId(userId)
                    .ttlSeconds((long) COOKIE_EXPIRE_SECONDS)
                    .build();
            callbackStateRepository.save(state);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest stored = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookies(request, response);
        return stored;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, AUTH_REQUEST_COOKIE_NAME);
    }

    public OAuthCallbackState getCallbackState(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String state = request.getParameter("state");
        if (state == null || state.isBlank()) {
            return null;
        }
        OAuthCallbackState stored = callbackStateRepository.findById(state).orElse(null);
        if (stored != null) {
            callbackStateRepository.deleteById(state);
            return stored;
        }
        return null;
    }

    private String resolveUserIdIfAuthenticated() {
        try {
            return AuthUtils.resolveUserId(SecurityContextHolder.getContext().getAuthentication());
        } catch (Exception ex) {
            return null;
        }
    }
}
