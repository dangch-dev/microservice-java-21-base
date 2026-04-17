package pl.co.identity.controller;

import org.springframework.web.bind.annotation.*;
import pl.co.common.dto.ApiResponse;
import pl.co.common.security.AuthUtils;
import pl.co.identity.dto.ProfileResponse;
import pl.co.identity.dto.UpdateProfileResponse;
import pl.co.identity.dto.UpdateProfileRequest;
import pl.co.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("""
        hasAnyAuthority(
            T(pl.co.common.security.RoleName).ROLE_GUEST.name(),
            T(pl.co.common.security.RoleName).ROLE_MEMBER.name(),
            T(pl.co.common.security.RoleName).ROLE_ADMIN.name(),
            T(pl.co.common.security.RoleName).ROLE_MANAGER.name()
        )
        """)
    public ApiResponse<ProfileResponse> me(Authentication authentication) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    @PreAuthorize("""
        hasAnyAuthority(
            T(pl.co.common.security.RoleName).ROLE_MEMBER.name(),
            T(pl.co.common.security.RoleName).ROLE_ADMIN.name(),
            T(pl.co.common.security.RoleName).ROLE_MANAGER.name()
        )
        """)
    public ApiResponse<UpdateProfileResponse> updateMe(Authentication authentication,
                                                       @Valid @RequestBody UpdateProfileRequest request) {
        String userId = AuthUtils.resolveUserId(authentication);
        return ApiResponse.ok(userService.updateProfile(userId, request));
    }
}
