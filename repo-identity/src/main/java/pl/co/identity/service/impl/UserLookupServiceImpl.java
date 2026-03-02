package pl.co.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.common.security.RoleName;
import pl.co.identity.dto.UserLookupResponse;
import pl.co.identity.entity.Role;
import pl.co.identity.entity.User;
import pl.co.identity.repository.UserRepository;
import pl.co.identity.service.UserLookupService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserLookupServiceImpl implements UserLookupService {

    private static final List<String> ROLE_PRIORITY = List.of(
            RoleName.ROLE_ADMIN.name(),
            RoleName.ROLE_MANAGER.name(),
            RoleName.ROLE_USER.name()
    );

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserLookupResponse> lookupByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<User> users = userRepository.findByIdIn(userIds);
        List<UserLookupResponse> result = new ArrayList<>();
        for (User user : users) {
            Set<String> roleNames = new HashSet<>();
            if (user.getRoles() != null) {
                for (Role role : user.getRoles()) {
                    if (role != null && role.getName() != null && !role.getName().isBlank()) {
                        roleNames.add(role.getName());
                    }
                }
            }
            result.add(UserLookupResponse.builder()
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .avatarUrl(user.getAvatarUrl())
                    .email(user.getEmail())
                    .roleName(resolveRoleName(roleNames))
                    .build());
        }
        return result;
    }

    private String resolveRoleName(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return null;
        }
        for (String candidate : ROLE_PRIORITY) {
            if (roleNames.contains(candidate)) {
                return candidate;
            }
        }
        return roleNames.stream().sorted().findFirst().orElse(null);
    }
}
