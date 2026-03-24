package pl.co.auth.service;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import pl.co.auth.dto.TokenResponse;

public interface OAuthLoginService {
    TokenResponse loginWithGoogle(OidcUser oidcUser);
}
