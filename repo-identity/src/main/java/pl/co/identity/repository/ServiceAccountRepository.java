package pl.co.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.identity.entity.ServiceAccount;

import java.util.Optional;

public interface ServiceAccountRepository extends JpaRepository<ServiceAccount, String> {
    Optional<ServiceAccount> findByClientIdAndEnabledTrueAndDeletedFalse(String clientId);
}
