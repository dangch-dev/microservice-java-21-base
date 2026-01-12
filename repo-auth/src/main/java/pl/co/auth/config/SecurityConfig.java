package pl.co.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import pl.co.common.filter.BearerTokenAuthenticationFilter;
import pl.co.common.util.RsaKeyUtil;
import pl.co.common.web.AccessDeniedHandler;
import pl.co.common.web.AuthenticationEntryPoint;
import pl.co.common.web.RequestContextFilter;

import java.util.List;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationEntryPoint restAuthenticationEntryPoint,
                                                   AccessDeniedHandler restAccessDeniedHandler,
                                                   BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/email/**").authenticated()
                        .requestMatchers("/signup", "/login", "/refresh", "/logout").permitAll()
                        .requestMatchers("/password/**", "/oauth/**").permitAll()
                        .anyRequest().permitAll())
                .addFilterBefore(commonRequestContextFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(RSAPublicKey jwtPublicKey) {
        return new BearerTokenAuthenticationFilter(jwtPublicKey, List.of(
                "/signup",
                "/login",
                "/refresh",
                "/logout",
                "/password/**",
                "/oauth/**"
        ));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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

    @Bean
    public RSAPrivateKey jwtPrivateKey(@Value("${security.external-jwt.private-key-path}") String privateKeyPath) {
        return RsaKeyUtil.loadPrivateKey(privateKeyPath);
    }
}

