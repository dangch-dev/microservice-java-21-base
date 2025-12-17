package pl.co.identity.repository;

import org.springframework.data.repository.CrudRepository;
import pl.co.identity.entity.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository extends CrudRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUserId(String userId);
}
