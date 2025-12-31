package pl.co.identity.service;

import pl.co.identity.dto.ServiceTokenResponse;

public interface ServiceTokenService {
    ServiceTokenResponse issueToken(String grantType, String clientId, String clientSecret);
}
