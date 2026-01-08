package pl.co.identity.service.impl;

import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.identity.dto.AdminCreateUserRequest;
import pl.co.identity.dto.AdminUpdateUserRequest;
import pl.co.identity.dto.AdminUserFilterRequest;
import pl.co.identity.dto.AdminUserPageResponse;
import pl.co.identity.dto.AdminUserResponse;
import pl.co.identity.entity.Role;
import pl.co.identity.entity.User;
import pl.co.identity.mapper.UserMapper;
import pl.co.identity.repository.RoleRepository;
import pl.co.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import pl.co.identity.service.AdminService;
import pl.co.common.security.RoleName;
import pl.co.common.security.UserStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    @Override
    public AdminUserPageResponse listUsers(AdminUserFilterRequest filter) {
        PageRequest page = PageRequest.of(filter.getPage(), filter.getSize());
        Specification<User> spec = buildSpec(filter);
        Page<User> result = userRepository.findAll(spec, page);
        List<AdminUserResponse> items = result.getContent().stream()
                .map(userMapper::toAdmin)
                .toList();
        return AdminUserPageResponse.builder()
                .items(items)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public AdminUserResponse getUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));
        return userMapper.toAdmin(user);
    }

    @Transactional
    @Override
    public AdminUserResponse createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(ErrorCode.CONFLICT, "Email already in use");
        }
        Set<Role> roles = resolveRoles(request.getRoles());
        if (roles.isEmpty()) {
            Role defaultRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                    .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_USER"));
            roles.add(defaultRole);
        }
        String status = request.getStatus() == null ? UserStatus.ACTIVE.name() : request.getStatus().name();
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .avatarUrl(request.getAvatarUrl())
                .address(request.getAddress())
                .status(status)
                .emailVerified(true)
                .roles(roles)
                .build();
        User saved = userRepository.save(user);
        return userMapper.toAdmin(saved);
    }

    @Transactional
    @Override
    public AdminUserResponse updateUser(String userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));

        if (request.getStatus() != null) {
            user.setStatus(request.getStatus().name());
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(resolveRoles(request.getRoles()));
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        User saved = userRepository.save(user);
        return userMapper.toAdmin(saved);
    }

    @Transactional
    @Override
    public void deleteUser(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(userId);
    }

    @Transactional
    @Override
    public AdminUserResponse activateUser(String userId) {
        return updateStatus(userId, UserStatus.ACTIVE);
    }

    @Transactional
    @Override
    public AdminUserResponse blockUser(String userId) {
        return updateStatus(userId, UserStatus.BLOCKED);
    }

    private AdminUserResponse updateStatus(String userId, UserStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "User not found"));
        user.setStatus(status.name());
        User saved = userRepository.save(user);
        return userMapper.toAdmin(saved);
    }

    private Set<Role> resolveRoles(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return new HashSet<>();
        }

        return names.stream()
                .map(name -> {
                    if (!isValidRoleName(name)) {
                        throw new ApiException(
                                ErrorCode.E221,
                                "Role not found data: " + name
                        );
                    }

                    Role role = new Role();
                    role.setName(RoleName.valueOf(name).name());
                    return role;
                })
                .collect(Collectors.toSet());
    }

    private boolean isValidRoleName(String name) {
        try {
            RoleName.valueOf(name);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Specification<User> buildSpec(AdminUserFilterRequest filter) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<>();
            if (filter.getEmailContains() != null && !filter.getEmailContains().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + filter.getEmailContains().toLowerCase() + "%"));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus().name()));
            }
            if (filter.getRole() != null && !filter.getRole().isBlank()) {
                var join = root.join("roles");
                predicates.add(cb.equal(join.get("name"), filter.getRole()));
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
