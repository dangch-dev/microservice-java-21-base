package pl.co.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.service.JwtTokenService;
import pl.co.auth.service.OAuthService;

@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final JwtTokenService jwtTokenService;

    @Override
    public TokenResponse issueInternalToken(String clientId, String clientSecret) {
        return jwtTokenService.issueInternalToken(clientId, clientSecret);
    }
}
