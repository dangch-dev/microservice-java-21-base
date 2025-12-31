package pl.co.identity.controller;

import org.springframework.web.bind.annotation.*;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthPrincipal;
import pl.co.identity.dto.ProfileResponse;
import pl.co.identity.dto.UpdateProfileRequest;
import pl.co.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<ProfileResponse> me(Authentication authentication) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        return ApiResponse.ok(userService.getProfile(principal.userId()));
    }

    @PutMapping("/me")
    public ApiResponse<ProfileResponse> updateMe(Authentication authentication,
                                                 @Valid @RequestBody UpdateProfileRequest request) {
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        return ApiResponse.ok(userService.updateProfile(principal.userId(), request));
    }
}
