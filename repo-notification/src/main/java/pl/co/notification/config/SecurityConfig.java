package pl.co.notification.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.co.common.filter.BearerTokenAuthenticationFilter;
import pl.co.common.filter.EmailVerifiedFilter;
import pl.co.common.util.RsaKeyUtil;

import java.security.interfaces.RSAPublicKey;
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
                                                   EmailVerifiedFilter emailVerifiedFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(emailVerifiedFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(RSAPublicKey jwtPublicKey) {
        return new BearerTokenAuthenticationFilter(jwtPublicKey,
                java.util.List.of()); // optional auth paths
    }

    @Bean
    public EmailVerifiedFilter emailVerifiedFilter() {
        return new EmailVerifiedFilter(java.util.List.of());
    }

    @Bean
    public RSAPublicKey jwtPublicKey(@Value("${security.external-jwt.public-key-path}") String publicKeyPath) {
        return RsaKeyUtil.loadPublicKey(publicKeyPath);
    }
}
