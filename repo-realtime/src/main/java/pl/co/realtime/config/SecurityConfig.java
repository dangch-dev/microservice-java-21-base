package pl.co.realtime.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pl.co.common.filter.InternalJwtFilter;
import pl.co.common.util.RsaKeyUtil;
import pl.co.common.web.AccessDeniedHandler;
import pl.co.common.filter.EmailVerifiedFilter;

import java.security.interfaces.RSAPublicKey;
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationEntryPoint restAuthenticationEntryPoint,
                                                   AccessDeniedHandler restAccessDeniedHandler,
                                                   InternalJwtFilter internalJwtFilter,
                                                   EmailVerifiedFilter emailVerifiedFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .addFilterBefore(internalJwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(emailVerifiedFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public EmailVerifiedFilter emailVerifiedFilter() {
        return new EmailVerifiedFilter(java.util.List.of());
    }

    @Bean
    public InternalJwtFilter internalJwtFilter(RSAPublicKey jwtPublicKey) {
        return new InternalJwtFilter(jwtPublicKey, java.util.List.of());
    }

    @Bean
    public RSAPublicKey jwtPublicKey(@Value("${security.external-jwt.public-key-path}") String publicKeyPath) {
        return RsaKeyUtil.loadPublicKey(publicKeyPath);
    }
}
