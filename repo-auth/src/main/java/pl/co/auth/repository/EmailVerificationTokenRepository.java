package pl.co.auth.repository;

import org.springframework.data.repository.CrudRepository;
import pl.co.auth.entity.EmailVerificationToken;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends CrudRepository<EmailVerificationToken, String> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUserId(String userId);
}

