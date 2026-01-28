package pl.co.identity.mapper;

import pl.co.identity.dto.AdminUserResponse;
import pl.co.identity.dto.ProfileResponse;
import pl.co.identity.entity.Role;
import pl.co.common.security.UserStatus;
import pl.co.identity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(toRoleNames(user.getRoles()))")
    ProfileResponse toProfile(User user);

    @Mapping(target = "roles", expression = "java(toRoleNames(user.getRoles()))")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    AdminUserResponse toAdmin(User user);

    default Set<String> toRoleNames(Set<Role> roles) {
        return roles == null
                ? Set.of()
                : roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    default UserStatus toUserStatus(String status) {
        return status == null ? null : UserStatus.valueOf(status);
    }

}
