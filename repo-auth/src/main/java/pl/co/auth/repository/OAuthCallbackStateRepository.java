package pl.co.auth.repository;

import org.springframework.data.repository.CrudRepository;
import pl.co.auth.entity.OAuthCallbackState;

public interface OAuthCallbackStateRepository extends CrudRepository<OAuthCallbackState, String> {
}
