package pl.co.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import pl.co.auth.dto.RefreshTokenRequest;
import pl.co.auth.dto.TokenResponse;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;

import java.time.Duration;

@Component
public class AuthCookieService {

    @Value("${app.security.cookies.access-token-name}")
    private String accessTokenCookieName;

    @Value("${app.security.cookies.refresh-token-name}")
    private String refreshTokenCookieName;

    @Value("${app.security.cookies.domain}")
    private String cookieDomain;

    @Value("${app.security.cookies.same-site}")
    private String sameSite;

    @Value("${app.security.cookies.secure}")
    private boolean secure;

    public void setTokens(HttpServletResponse response, TokenResponse tokens) {
        addCookie(response, accessTokenCookieName, tokens.getAccessToken(),
                Duration.ofSeconds(tokens.getAcssessExpireIn()));
        addCookie(response, refreshTokenCookieName, tokens.getRefreshToken(),
                Duration.ofSeconds(tokens.getRefreshExpireIn()));
    }

    public void clearTokens(HttpServletResponse response) {
        addCookie(response, accessTokenCookieName, "", Duration.ZERO);
        addCookie(response, refreshTokenCookieName, "", Duration.ZERO);
    }

    public String resolveRefreshToken(HttpServletRequest request, RefreshTokenRequest body) {
        if (body != null && StringUtils.hasText(body.getRefreshToken())) {
            return body.getRefreshToken();
        }
        String cookieToken = getRefreshTokenIfPresent(request);
        if (StringUtils.hasText(cookieToken)) {
            return cookieToken;
        }
        throw new ApiException(ErrorCode.E243, "Required parameter missing: refreshToken");
    }

    public String getRefreshTokenIfPresent(HttpServletRequest request) {
        return CookieUtils.getCookie(request, refreshTokenCookieName)
                .map(jakarta.servlet.http.Cookie::getValue)
                .orElse(null);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(sameSite);
        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
