package pl.co.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.co.common.exception.ApiException;
import pl.co.common.jwt.JwtVerifier;
import pl.co.common.jwt.record.JwtPayload;
import pl.co.common.jwt.record.JwtVerificationOptions;
import pl.co.common.filter.principal.AuthPrincipal;
import pl.co.common.security.RoleName;
import pl.co.common.security.SecurityConstants;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Common JWT filter:
 * - All paths are treated as required by default.
 * - Optional paths suppress warn logs when token is missing/invalid.
 * Access control is decided by SecurityConfig.
 */
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BearerTokenAuthenticationFilter.class);
    private static final List<String> AUDIENCE = List.of(SecurityConstants.AUD_EXTERNAL);
    private final RSAPublicKey publicKey;
    private final List<String> optionalPatterns;
    private final JwtVerificationOptions verificationOptions;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public BearerTokenAuthenticationFilter(RSAPublicKey publicKey,
                                           List<String> optionalPatterns) {
        this.publicKey = publicKey;
        this.optionalPatterns = optionalPatterns == null ? Collections.emptyList() : optionalPatterns;
        this.verificationOptions = new JwtVerificationOptions(
                AUDIENCE,
                SecurityConstants.TYP_ACCESS);
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(SecurityConstants.HEADER_AUTHORIZATION);
        boolean isOptional = matches(request.getRequestURI(), optionalPatterns);

        if (header == null || !header.startsWith(SecurityConstants.HEADER_BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(SecurityConstants.HEADER_BEARER_PREFIX.length()).trim();
        try {
            JwtPayload payload = JwtVerifier.verify(token, publicKey, verificationOptions);
            AuthPrincipal principal = new AuthPrincipal(payload.userId(), payload.email(), payload.emailVerified(), payload.roles());
            boolean hasInternalRole = SecurityContextHolder.getContext().getAuthentication() != null
                    && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> RoleName.ROLE_INTERNAL.name().equals(a.getAuthority()));
            List<SimpleGrantedAuthority> authorities = payload.roles().stream()
                    .filter(Objects::nonNull)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toCollection(ArrayList::new));
            if (hasInternalRole && authorities.stream().noneMatch(a -> RoleName.ROLE_INTERNAL.name().equals(a.getAuthority()))) {
                authorities.add(new SimpleGrantedAuthority(RoleName.ROLE_INTERNAL.name()));
            }
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ApiException ex) {
            if (!isOptional) {
                log.warn("JWT verification failed on path {}: {}", request.getRequestURI(), ex.getMessage());
            }
            // Optional paths: invalid token won't set auth; access is decided by SecurityConfig.
        }
        filterChain.doFilter(request, response);
    }

    private boolean matches(String path, List<String> patterns) {
        return patterns.stream().anyMatch(p -> matcher.match(p, path) || path.startsWith(p));
    }

}
