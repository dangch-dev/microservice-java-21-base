package pl.co.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.auth.entity.UserOAuth;

import java.util.Optional;

public interface UserOAuthRepository extends JpaRepository<UserOAuth, String> {
    Optional<UserOAuth> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<UserOAuth> findByProviderAndEmail(String provider, String email);
}
