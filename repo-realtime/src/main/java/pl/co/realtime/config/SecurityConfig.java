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
import pl.co.common.util.RsaKeyUtil;
import pl.co.common.web.AccessDeniedHandler;

import java.security.interfaces.RSAPublicKey;
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationEntryPoint restAuthenticationEntryPoint,
                                                   AccessDeniedHandler restAccessDeniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public RSAPublicKey jwtPublicKey(@Value("${security.external-jwt.public-key-path}") String publicKeyPath) {
        return RsaKeyUtil.loadPublicKey(publicKeyPath);
    }
}
