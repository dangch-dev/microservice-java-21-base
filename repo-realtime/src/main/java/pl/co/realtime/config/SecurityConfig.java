package pl.co.realtime.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.co.common.security.BaseJwtFilter;
import pl.co.common.security.EmailVerifiedFilter;
import pl.co.common.security.RsaKeyUtil;

import java.security.interfaces.RSAPublicKey;
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   BaseJwtFilter baseJwtFilter,
                                                   EmailVerifiedFilter emailVerifiedFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/entry/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(baseJwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(emailVerifiedFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public BaseJwtFilter baseJwtFilter(RSAPublicKey jwtPublicKey) {
        return new BaseJwtFilter(jwtPublicKey,
                java.util.List.of("/entry/**"), // skip (ws handshake)
                java.util.List.of());         // no optional paths
    }

    @Bean
    public EmailVerifiedFilter emailVerifiedFilter() {
        return new EmailVerifiedFilter(java.util.List.of("/entry/**"));
    }

    @Bean
    public RSAPublicKey jwtPublicKey(@org.springframework.beans.factory.annotation.Value("${security.jwt.public-key-path}") String publicKeyPath) {
        return RsaKeyUtil.loadPublicKey(publicKeyPath);
    }
}
