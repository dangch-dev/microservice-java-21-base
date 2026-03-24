package pl.co.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import pl.co.auth.repository.OAuthCallbackStateRepository;
import pl.co.common.filter.BearerTokenAuthenticationFilter;
import pl.co.common.filter.EmailVerifiedFilter;
import pl.co.common.filter.InternalJwtFilter;
import pl.co.common.util.RsaKeyUtil;
import pl.co.common.web.AccessDeniedHandler;
import pl.co.common.web.AuthenticationEntryPoint;
import pl.co.common.web.RequestContextFilter;
import pl.co.auth.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import pl.co.auth.oauth.GoogleOfflineAuthorizationRequestResolver;
import pl.co.auth.oauth.OAuth2AuthenticationFailureHandler;
import pl.co.auth.oauth.OAuth2AuthenticationSuccessHandler;

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
                                                   BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter,
                                                   InternalJwtFilter internalJwtFilter,
                                                   EmailVerifiedFilter emailVerifiedFilter,
                                                   OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                                                   OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
                                                   HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
                                                   OAuth2AuthorizationRequestResolver authorizationRequestResolver,
                                                   ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/email/**").authenticated()
                        .requestMatchers("/signup", "/guest", "/signin", "/refresh", "/signout").permitAll()
                        .requestMatchers("/password/**", "/oauth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().permitAll())
                .addFilterBefore(commonRequestContextFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(internalJwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(bearerTokenAuthenticationFilter, OAuth2AuthorizationRequestRedirectFilter.class)
                .addFilterBefore(emailVerifiedFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults());

        if (clientRegistrationRepository.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth
                    .authorizationEndpoint(a -> a.authorizationRequestRepository(authorizationRequestRepository)
                            .authorizationRequestResolver(authorizationRequestResolver))
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(oAuth2AuthenticationFailureHandler));
        }
        return http.build();
    }

    @Bean
    public BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(RSAPublicKey jwtPublicKey) {
        return new BearerTokenAuthenticationFilter(jwtPublicKey, List.of(
                "/signup",
                "/guest",
                "/signin",
                "/refresh",
                "/signout",
                "/password/**",
                "/oauth/**", // internal login
                "/oauth2/**", // login with google
                "/login/oauth2/**" // google oauth2 callback
        ));
    }

    @Bean
    public EmailVerifiedFilter emailVerifiedFilter() {
        return new EmailVerifiedFilter(List.of(
                "/signup",
                "/guest",
                "/signin",
                "/refresh",
                "/signout",
                "/password/**",
                "/oauth/**",
                "/oauth2/**",
                "/login/oauth2/**",
                "/email/**"
        ));
    }

    @Bean
    public InternalJwtFilter internalJwtFilter(RSAPublicKey jwtPublicKey) {
        return new InternalJwtFilter(jwtPublicKey, List.of());
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
    public HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository(
            OAuthCallbackStateRepository callbackStateRepository) {
        return new HttpCookieOAuth2AuthorizationRequestRepository(callbackStateRepository);
    }

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new GoogleOfflineAuthorizationRequestResolver(clientRegistrationRepository);
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
