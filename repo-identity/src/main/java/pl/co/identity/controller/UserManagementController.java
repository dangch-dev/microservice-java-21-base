package pl.co.identity.controller;

import pl.co.common.dto.ApiResponse;
import pl.co.identity.dto.AdminCreateUserRequest;
import pl.co.identity.dto.AdminUpdateUserRequest;
import pl.co.identity.dto.AdminUserFilterRequest;
import pl.co.identity.dto.AdminUserPageResponse;
import pl.co.identity.dto.AdminUserResponse;
import pl.co.identity.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN')")
public class UserManagementController {

    private final AdminService adminService;

    @GetMapping
    public ApiResponse<AdminUserPageResponse> list(@ModelAttribute AdminUserFilterRequest filter) {
        return ApiResponse.ok(adminService.listUsers(filter));
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> detail(@PathVariable("id") String id) {
        return ApiResponse.ok(adminService.getUser(id));
    }

    @PostMapping
    public ApiResponse<AdminUserResponse> create(@Valid @RequestBody AdminCreateUserRequest request) {
        return ApiResponse.ok(adminService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminUserResponse> update(@PathVariable("id") String id,
                                                 @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.ok(adminService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") String id) {
        adminService.deleteUser(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<AdminUserResponse> activate(@PathVariable("id") String id) {
        return ApiResponse.ok(adminService.activateUser(id));
    }

    @PostMapping("/{id}/block")
    public ApiResponse<AdminUserResponse> block(@PathVariable("id") String id) {
        return ApiResponse.ok(adminService.blockUser(id));
    }
}
