package pl.co.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.service.OAuthService;
import pl.co.common.dto.ApiResponse;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;

    @PostMapping("/token")
    public ApiResponse<TokenResponse> token(@RequestParam("client_id") String clientId,
                                                   @RequestParam("client_secret") String clientSecret) {
        return ApiResponse.ok(oAuthService.issueInternalToken(clientId, clientSecret));
    }
}
