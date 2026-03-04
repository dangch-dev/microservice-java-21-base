package pl.co.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.co.auth.dto.GoogleConnectTokenResponse;
import pl.co.auth.service.GoogleConnectTokenService;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;

@RestController
@RequestMapping("/google/connect")
@RequiredArgsConstructor
public class GoogleConnectController {

    private final GoogleConnectTokenService googleConnectTokenService;

    @GetMapping("/token")
    public ApiResponse<GoogleConnectTokenResponse> token(Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        GoogleConnectTokenResponse token = googleConnectTokenService.getToken(userId);
        return ApiResponse.ok(token);
    }
}
