package pl.co.storage.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import pl.co.common.filter.BearerTokenAuthenticationFilter;
import pl.co.common.filter.EmailVerifiedFilter;
import pl.co.common.filter.InternalJwtFilter;
import pl.co.common.util.RsaKeyUtil;
import pl.co.common.web.RequestContextFilter;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
                                                   InternalJwtFilter internalJwtFilter,
                                                   EmailVerifiedFilter emailVerifiedFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/internal/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(commonRequestContextFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalJwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(emailVerifiedFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(RSAPublicKey jwtPublicKey) {
        return new BearerTokenAuthenticationFilter(jwtPublicKey,
                List.of("/internal/**"));
    }

    @Bean
    public EmailVerifiedFilter emailVerifiedFilter() {
        return new EmailVerifiedFilter(List.of("/internal/**"));
    }

    @Bean
    public InternalJwtFilter internalJwtFilter(RSAPublicKey jwtPublicKey) {
        return new InternalJwtFilter(
                jwtPublicKey,
                List.of("/internal/**"));
    }

    @Bean
    public RequestContextFilter commonRequestContextFilter() {
        return new RequestContextFilter();
    }

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeHeaders(false);
        filter.setIncludePayload(false);
        filter.setIncludeQueryString(true);
        return filter;
    }

    @Bean
    public RSAPublicKey jwtPublicKey(@Value("${security.external-jwt.public-key-path}") String publicKeyPath) {
        return RsaKeyUtil.loadPublicKey(publicKeyPath);
    }
}
