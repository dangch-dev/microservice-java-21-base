package pl.co.identity.security;

import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.security.AuthPrincipal;
import pl.co.common.security.JwtUtils;
import pl.co.common.security.SecurityConstants;
import pl.co.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${security.jwt.secret}")
    private String jwtSecret;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(SecurityConstants.HEADER_AUTHORIZATION);
        if (header == null || !header.startsWith(SecurityConstants.HEADER_BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(SecurityConstants.HEADER_BEARER_PREFIX.length()).trim();
        try {
            JwtUtils.JwtPayload payload = JwtUtils.verify(token, jwtSecret);
            if (!payload.emailVerified() && !isEmailVerificationAllowed(request)) {
                writeError(response, ErrorCode.E233, "Email not verified");
                return;
            }
            AuthPrincipal principal = new AuthPrincipal(payload.userId(), payload.email(), payload.emailVerified(), payload.roles());
            List<SimpleGrantedAuthority> authorities = payload.roles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ApiException ex) {
            log.warn("JWT verification failed: {}", ex.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private boolean isEmailVerificationAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth");
    }

    private void writeError(HttpServletResponse response, ErrorCode code, String message) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType("application/json");
        ApiResponse<Void> api = ApiResponse.error(code.code(), message);
        response.getWriter().write(objectMapper.writeValueAsString(api));
    }
}
