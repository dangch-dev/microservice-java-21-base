package pl.co.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.co.auth.entity.ServiceAccount;

import java.util.Optional;

public interface ServiceAccountRepository extends JpaRepository<ServiceAccount, String> {
    Optional<ServiceAccount> findByClientIdAndEnabledTrueAndDeletedFalse(String clientId);
}

