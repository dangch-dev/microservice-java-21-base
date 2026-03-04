package pl.co.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.co.auth.service.GoogleOAuthTokenService;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;

@RestController
@RequestMapping("/google")
@RequiredArgsConstructor
public class GoogleDisconnectController {

    private final GoogleOAuthTokenService googleOAuthTokenService;

    @PostMapping("/disconnect")
    public ApiResponse<Void> disconnect(Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        googleOAuthTokenService.deleteByUserId(userId);
        return ApiResponse.ok(null);
    }
}
