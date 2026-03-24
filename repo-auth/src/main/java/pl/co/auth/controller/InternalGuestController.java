package pl.co.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.co.auth.dto.InternalGuestRequest;
import pl.co.auth.dto.InternalGuestResponse;
import pl.co.auth.service.AuthService;
import pl.co.common.dto.ApiResponse;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalGuestController {

    private final AuthService authService;

    @PostMapping("/guest")
    @PreAuthorize("hasAuthority(T(pl.co.common.security.RoleName).ROLE_INTERNAL.name())")
    public ApiResponse<InternalGuestResponse> upsertGuest(@Valid @RequestBody InternalGuestRequest request) {
        return ApiResponse.ok(authService.upsertGuest(request));
    }
}

