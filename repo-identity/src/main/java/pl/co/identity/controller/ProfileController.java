package pl.co.identity.controller;

import org.springframework.web.bind.annotation.*;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;
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
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ApiResponse<ProfileResponse> updateMe(Authentication authentication,
                                                 @Valid @RequestBody UpdateProfileRequest request) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(userService.updateProfile(userId, request));
    }
}
