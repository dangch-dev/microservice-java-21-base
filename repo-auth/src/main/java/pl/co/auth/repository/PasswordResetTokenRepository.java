package pl.co.auth.repository;

import org.springframework.data.repository.CrudRepository;
import pl.co.auth.entity.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenRepository extends CrudRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUserId(String userId);
}

