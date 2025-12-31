package pl.co.identity.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.co.common.dto.ApiResponse;
import pl.co.identity.dto.ServiceTokenResponse;
import pl.co.identity.service.ServiceTokenService;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final ServiceTokenService serviceTokenService;

    @PostMapping("/token")
    public ApiResponse<ServiceTokenResponse> token(@RequestParam("grant_type") String grantType,
                                                   @RequestParam("client_id") String clientId,
                                                   @RequestParam("client_secret") String clientSecret) {
        return ApiResponse.ok(serviceTokenService.issueToken(grantType, clientId, clientSecret));
    }
}
