package pl.co.auth.service;

import pl.co.auth.dto.RoleResponse;

import java.util.List;

public interface RoleService {
    List<RoleResponse> listRoles();
}
