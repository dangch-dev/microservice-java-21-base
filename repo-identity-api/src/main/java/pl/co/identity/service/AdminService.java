package pl.co.identity.service;

import pl.co.identity.dto.AdminCreateUserRequest;
import pl.co.identity.dto.AdminUpdateUserRequest;
import pl.co.identity.dto.AdminUserFilterRequest;
import pl.co.identity.dto.AdminUserPageResponse;
import pl.co.identity.dto.AdminUserResponse;

public interface AdminService {
    AdminUserPageResponse listUsers(AdminUserFilterRequest filter);
    AdminUserResponse getUser(String userId);
    AdminUserResponse createUser(AdminCreateUserRequest request);
    AdminUserResponse updateUser(String userId, AdminUpdateUserRequest request);
    void deleteUser(String userId);
    AdminUserResponse activateUser(String userId);
    AdminUserResponse blockUser(String userId);
}
