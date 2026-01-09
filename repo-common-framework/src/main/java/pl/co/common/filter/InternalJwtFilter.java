package pl.co.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.co.common.dto.ApiResponse;
import pl.co.common.jwt.record.JwtPayload;
import pl.co.common.jwt.record.JwtVerificationOptions;
import pl.co.common.jwt.JwtVerifier;
import pl.co.common.filter.principal.ServicePrincipal;
import pl.co.common.security.RoleName;
import pl.co.common.security.SecurityConstants;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InternalJwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalJwtFilter.class);
    private static final List<String> AUDIENCE = List.of(SecurityConstants.AUD_INTERNAL);

    private final RSAPublicKey publicKey;
    private final List<String> protectedPatterns;
    private final JwtVerificationOptions verificationOptions;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public InternalJwtFilter(RSAPublicKey publicKey,
                             List<String> protectedPatterns) {
        this.publicKey = publicKey;
        this.protectedPatterns = protectedPatterns == null ? Collections.emptyList() : protectedPatterns;
        this.verificationOptions = new JwtVerificationOptions(
                AUDIENCE,
                null);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return protectedPatterns.stream().noneMatch(p -> matcher.match(p, path) || path.startsWith(p));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(SecurityConstants.HEADER_INTERNAL_TOKEN);
        if (header == null || !header.startsWith(SecurityConstants.HEADER_BEARER_PREFIX)) {
            reject(response, "Missing internal token");
            return;
        }

        String token = header.substring(SecurityConstants.HEADER_BEARER_PREFIX.length()).trim();
        try {
            JwtPayload payload = JwtVerifier.verify(token, publicKey, verificationOptions);
            if (!StringUtils.hasText(payload.userId())) {
                throw new IllegalArgumentException("Missing subject");
            }
            ServicePrincipal principal = new ServicePrincipal(payload.userId(), Set.of());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority(RoleName.ROLE_INTERNAL.name())));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.warn("Internal JWT rejected: {}", ex.getMessage());
            reject(response, "Invalid internal token");
        }
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        ApiResponse<Void> body = ApiResponse.error("UNAUTHORIZED", message);
        response.getWriter().write("{\"success\":false,\"errorCode\":\"" + body.errorCode()
                + "\",\"errorMessage\":\"" + body.errorMessage() + "\"}");
    }
}
