package pl.co.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.co.common.util.StringUtils;
import pl.co.auth.dto.TokenResponse;
import pl.co.auth.entity.Role;
import pl.co.auth.entity.User;
import pl.co.auth.entity.UserOAuth;
import pl.co.auth.repository.RoleRepository;
import pl.co.auth.repository.UserOAuthRepository;
import pl.co.auth.repository.UserRepository;
import pl.co.auth.service.JwtTokenService;
import pl.co.auth.service.OAuthLoginService;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.security.RoleName;
import pl.co.common.security.UserStatus;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthLoginServiceImpl implements OAuthLoginService {

    private static final String PROVIDER_GOOGLE = "google";

    private final UserRepository userRepository;
    private final UserOAuthRepository userOAuthRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Transactional
    @Override
    public TokenResponse loginWithGoogle(OidcUser oidcUser) {
        if (oidcUser == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid oauth user");
        }

        String subject = oidcUser.getSubject();
        if (!StringUtils.hasText(subject)) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Missing subject");
        }

        String email = oidcUser.getEmail();
        if (!StringUtils.hasText(email)) {
            throw new ApiException(ErrorCode.E221, "Email not provided by provider");
        }

        UserOAuth userOAuth = findUserOAuthBySubject(subject);
        User user = resolveUserForGoogleLogin(userOAuth, oidcUser, email);

        boolean shouldUpdateUser = false;
        if (isGuestOnly(user)) {
            resetGuestOnlyUserAsGoogleMember(user);
            shouldUpdateUser = true;
        }
        ensureNotBlocked(user);

        if (syncUserProfileFromGoogle(user, oidcUser, email)) {
            shouldUpdateUser = true;
        }

        if (shouldUpdateUser) {
            userRepository.save(user);
        }

        if (userOAuth == null) {
            userOAuth = createUserOAuthMapping(subject, email, user.getId());
        } else if (syncUserOAuthEmail(userOAuth, email)) {
            userOAuthRepository.save(userOAuth);
        }

        return jwtTokenService.issueExternalTokens(user);
    }

    private UserOAuth findUserOAuthBySubject(String subject) {
        return userOAuthRepository.findByProviderAndProviderUserId(PROVIDER_GOOGLE, subject).orElse(null);
    }

    private User resolveUserForGoogleLogin(UserOAuth userOAuth, OidcUser oidcUser, String email) {
        if (userOAuth != null) {
            return userRepository.findById(userOAuth.getUserId())
                    .orElseThrow(() -> new ApiException(ErrorCode.E227, "No data found"));
        }
        Optional<User> existingByEmail = userRepository.findByEmail(email);
        return existingByEmail.orElseGet(() -> createUser(oidcUser, email));
    }

    private void ensureNotBlocked(User user) {
        if (UserStatus.BLOCKED.name().equals(user.getStatus())) {
            throw new ApiException(ErrorCode.E249);
        }
    }

    private boolean syncUserProfileFromGoogle(User user, OidcUser oidcUser, String email) {
        boolean updated = false;
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            updated = true;
        }

        String fullName = resolveFullName(oidcUser, email);
        if (StringUtils.hasText(fullName) && !fullName.equals(user.getFullName())) {
            user.setFullName(fullName);
            updated = true;
        }

        String picture = oidcUser.getPicture();
        if (StringUtils.hasText(picture) && !picture.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(picture);
            updated = true;
        }

        return updated;
    }

    private UserOAuth createUserOAuthMapping(String subject, String email, String userId) {
        UserOAuth mapping = UserOAuth.builder()
                .provider(PROVIDER_GOOGLE)
                .providerUserId(subject)
                .email(email)
                .userId(userId)
                .build();
        return userOAuthRepository.save(mapping);
    }

    private boolean syncUserOAuthEmail(UserOAuth userOAuth, String email) {
        if (email.equals(userOAuth.getEmail())) {
            return false;
        }
        userOAuth.setEmail(email);
        return true;
    }

    private boolean isGuestOnly(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        long guestCount = user.getRoles().stream()
                .filter(role -> role != null && RoleName.ROLE_GUEST.name().equals(role.getName()))
                .count();
        return guestCount == 1 && user.getRoles().size() == 1;
    }

    private void ensureMemberRole(User user) {
        boolean hasMemberRole = user.getRoles().stream()
                .anyMatch(role -> role != null && RoleName.ROLE_MEMBER.name().equals(role.getName()));
        if (hasMemberRole) {
            return;
        }
        Role userRole = roleRepository.findByName(RoleName.ROLE_MEMBER.name())
                .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_MEMBER"));
        user.getRoles().add(userRole);
    }

    private void resetGuestOnlyUserAsGoogleMember(User user) {
        ensureMemberRole(user);
        user.setEmailVerified(true);
    }

    private User createUser(OidcUser oidcUser, String email) {
        Role userRole = roleRepository.findByName(RoleName.ROLE_MEMBER.name())
                .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_MEMBER"));

        String fullName = resolveFullName(oidcUser, email);
        String password = passwordEncoder.encode(UUID.randomUUID().toString());
        User user = User.builder()
                .email(email)
                .password(password)
                .fullName(fullName)
                .phoneNumber(null)
                .avatarUrl(oidcUser.getPicture())
                .address(null)
                .status(UserStatus.ACTIVE.name())
                .emailVerified(true)
                .build();
        user.getRoles().add(userRole);
        return userRepository.save(user);
    }

    private String resolveFullName(OidcUser oidcUser, String email) {
        String fullName = oidcUser.getFullName();
        if (StringUtils.hasText(fullName)) {
            return fullName;
        }
        String given = oidcUser.getGivenName();
        String family = oidcUser.getFamilyName();
        if (StringUtils.hasText(given) && StringUtils.hasText(family)) {
            return given + " " + family;
        }
        if (StringUtils.hasText(given)) {
            return given;
        }
        if (StringUtils.hasText(family)) {
            return family;
        }
        return email;
    }
}
