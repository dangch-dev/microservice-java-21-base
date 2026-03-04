package pl.co.auth.repository;

import org.springframework.data.repository.CrudRepository;
import pl.co.auth.entity.GoogleOAuthToken;

import java.util.Optional;

public interface GoogleOAuthTokenRepository extends CrudRepository<GoogleOAuthToken, String> {
    Optional<GoogleOAuthToken> findFirstByUserId(String userId);

    void deleteByUserId(String userId);
}
