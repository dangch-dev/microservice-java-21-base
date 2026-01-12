package pl.co.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.co.auth.dto.RoleResponse;
import pl.co.auth.service.RoleService;
import pl.co.common.dto.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority(T(pl.co.common.security.RoleName).ROLE_ADMIN.name())")
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.ok(roleService.listRoles());
    }
}
