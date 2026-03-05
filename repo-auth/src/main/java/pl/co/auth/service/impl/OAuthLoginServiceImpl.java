package pl.co.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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

        UserOAuth userOAuth = userOAuthRepository.findByProviderAndProviderUserId(PROVIDER_GOOGLE, subject)
                .orElse(null);
        User user = null;

        if (userOAuth != null) {
            user = userRepository.findById(userOAuth.getUserId())
                    .orElseThrow(() -> new ApiException(ErrorCode.E227, "No data found"));
        } else {
            Optional<User> existingByEmail = userRepository.findByEmail(email);
            if (existingByEmail.isPresent()) {
                user = existingByEmail.get();
            } else {
                user = createUser(oidcUser, email);
            }
            UserOAuth mapping = UserOAuth.builder()
                    .provider(PROVIDER_GOOGLE)
                    .providerUserId(subject)
                    .email(email)
                    .userId(user.getId())
                    .build();
            userOAuthRepository.save(mapping);
            userOAuth = mapping;
        }

        if (UserStatus.BLOCKED.name().equals(user.getStatus())) {
            throw new ApiException(ErrorCode.E249);
        }

        boolean shouldUpdate = false;
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            shouldUpdate = true;
        }
        if (StringUtils.hasText(oidcUser.getFullName()) && !oidcUser.getFullName().equals(user.getFullName())) {
            user.setFullName(oidcUser.getFullName());
            shouldUpdate = true;
        }
        String picture = oidcUser.getPicture();
        if (StringUtils.hasText(picture) && !picture.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(picture);
            shouldUpdate = true;
        }
        if (shouldUpdate) {
            userRepository.save(user);
        }

        if (userOAuth != null && !email.equals(userOAuth.getEmail())) {
            userOAuth.setEmail(email);
            userOAuthRepository.save(userOAuth);
        }

        return jwtTokenService.issueExternalTokens(user);
    }

    private User createUser(OidcUser oidcUser, String email) {
        Role userRole = roleRepository.findByName(RoleName.ROLE_MEMBER.name())
                .orElseThrow(() -> new ApiException(ErrorCode.E221, "Role not found data: ROLE_MEMBER"));

        String fullName = resolveFullName(oidcUser, email);
        String password = passwordEncoder.encode(java.util.UUID.randomUUID().toString());
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
