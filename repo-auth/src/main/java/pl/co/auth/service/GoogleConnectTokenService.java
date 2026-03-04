package pl.co.auth.service;

import pl.co.auth.dto.GoogleConnectTokenResponse;

public interface GoogleConnectTokenService {
    GoogleConnectTokenResponse getToken(String userId);
}
