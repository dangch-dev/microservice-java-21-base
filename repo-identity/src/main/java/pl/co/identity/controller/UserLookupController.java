package pl.co.identity.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.co.common.dto.ApiResponse;
import pl.co.identity.dto.UserLookupPageResponse;
import pl.co.identity.dto.UserLookupRequest;
import pl.co.identity.dto.UserLookupResponse;
import pl.co.identity.dto.UserLookupSearchRequest;
import pl.co.identity.service.UserLookupService;


import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserLookupController {

    private final UserLookupService userLookupService;

    @PostMapping("/internal/users/lookup")
    @PreAuthorize("hasAnyAuthority(T(pl.co.common.security.RoleName).ROLE_INTERNAL.name())")
    public ApiResponse<List<UserLookupResponse>> lookup(@Valid @RequestBody UserLookupRequest request) {
        return ApiResponse.ok(userLookupService.lookupByIds(request.getUserIds()));
    }

    @PostMapping("/users/lookup")
    @PreAuthorize("hasAnyAuthority(T(pl.co.common.security.RoleName).ROLE_ADMIN.name())")
    public ApiResponse<UserLookupPageResponse> search(@Valid @RequestBody UserLookupSearchRequest request) {
        return ApiResponse.ok(userLookupService.search(
                request.getSearchValue(),
                request.getRoleNames(),
                request.getPage(),
                request.getSize()
        ));
    }
}
