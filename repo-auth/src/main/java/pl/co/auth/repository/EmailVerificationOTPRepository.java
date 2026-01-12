package pl.co.auth.repository;

import org.springframework.data.repository.CrudRepository;
import pl.co.auth.entity.EmailVerificationOTP;

import java.util.Optional;

public interface EmailVerificationOTPRepository extends CrudRepository<EmailVerificationOTP, String> {
    Optional<EmailVerificationOTP> findByUserId(String userId);
    void deleteByUserId(String userId);
}
