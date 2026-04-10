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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import pl.co.common.util.StringUtils;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserLookupServiceImpl implements UserLookupService {

    private static final List<String> ROLE_PRIORITY = List.of(
            RoleName.ROLE_ADMIN.name(),
            RoleName.ROLE_MANAGER.name(),
            RoleName.ROLE_MEMBER.name()
    );

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserLookupResponse> lookupByIds(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<User> users = userRepository.findByIdIn(userIds);
        return toLookupResponses(users);
    }

    @Override
    @Transactional(readOnly = true)
    public pl.co.identity.dto.UserLookupPageResponse search(String searchValue,
                                                            List<String> roleNames,
                                                            Integer page,
                                                            Integer size) {
        String normalizedSearch = StringUtils.trimToEmpty(searchValue);
        List<String> normalizedRoleNames = normalizeRoleNames(roleNames);
        int pageValue = Math.max(page == null ? 0 : page, 0);
        int sizeValue = Math.max(size == null ? 20 : size, 1);
        PageRequest pageRequest = PageRequest.of(pageValue, sizeValue, Sort.by("createdAt").descending());
        Specification<User> spec = (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(normalizedSearch)) {
                String likeValue = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
                Predicate byName = cb.like(cb.lower(root.get("fullName")), likeValue);
                Predicate byEmail = cb.like(cb.lower(root.get("email")), likeValue);
                predicates.add(cb.or(byName, byEmail));
            }
            if (!normalizedRoleNames.isEmpty()) {
                predicates.add(root.join("roles").get("name").in(normalizedRoleNames));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> result = userRepository.findAll(spec, pageRequest);
        List<UserLookupResponse> items = toLookupResponses(result.getContent());
        return pl.co.identity.dto.UserLookupPageResponse.builder()
                .items(items)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .page(result.getNumber())
                .size(result.getSize())
                .build();
    }

    private List<String> normalizeRoleNames(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String roleName : roleNames) {
            if (!StringUtils.hasText(roleName)) {
                continue;
            }
            String candidate = roleName.trim().toUpperCase(Locale.ROOT);
            if (seen.add(candidate)) {
                normalized.add(candidate);
            }
        }
        return normalized;
    }

    private List<UserLookupResponse> toLookupResponses(List<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
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
                    .phoneNumber(user.getPhoneNumber())
                    .roleNames(resolveRoleNames(roleNames))
                    .build());
        }
        return result;
    }

    private List<String> resolveRoleNames(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }
        Set<String> remaining = new HashSet<>(roleNames);
        List<String> ordered = new ArrayList<>();
        for (String candidate : ROLE_PRIORITY) {
            if (remaining.remove(candidate)) {
                ordered.add(candidate);
            }
        }
        if (!remaining.isEmpty()) {
            ordered.addAll(remaining.stream().sorted().toList());
        }
        return ordered;
    }
}
