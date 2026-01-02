package pl.co.common.jpa;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pl.co.common.security.principal.AuthPrincipal;
import pl.co.common.security.principal.ServicePrincipal;

import java.util.Optional;

@AutoConfiguration
@EnableJpaAuditing
@ConditionalOnClass({AuditingEntityListener.class, EntityManagerFactory.class})
@ConditionalOnBean(EntityManagerFactory.class)
public class AuditingAutoConfiguration {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof AuthPrincipal authPrincipal) {
                return Optional.ofNullable(authPrincipal.userId());
            }
            if (principal instanceof ServicePrincipal servicePrincipal) {
                return Optional.ofNullable(servicePrincipal.serviceName());
            }
            if (principal instanceof String value && !"anonymousUser".equals(value)) {
                return Optional.of(value);
            }
            return Optional.empty();
        };
    }
}
