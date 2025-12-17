package pl.co.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import pl.co.apigateway.config.GatewaySecurityProperties;
import pl.co.common.exception.ApiException;
import pl.co.common.security.JwtUtils;
import pl.co.common.security.SecurityConstants;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final GatewaySecurityProperties securityProperties;

    public JwtAuthenticationFilter(GatewaySecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPermitPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(SecurityConstants.HEADER_AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.HEADER_BEARER_PREFIX)) {
            return securityProperties.isRequired() ? unauthorized(exchange, "Missing bearer token") : chain.filter(exchange);
        }
        String token = authHeader.substring(SecurityConstants.HEADER_BEARER_PREFIX.length()).trim();
        JwtUtils.JwtPayload payload;
        try {
            payload = JwtUtils.verify(token, securityProperties.getSecret());
        } catch (ApiException ex) {
            log.warn("JWT verification failed: {}", ex.getMessage());
            return unauthorized(exchange, ex.getMessage());
        }
        if (!payload.emailVerified()) {
            return forbidden(exchange, "Email not verified");
        }
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(SecurityConstants.HEADER_USER_ID, payload.userId())
                .header(SecurityConstants.HEADER_ROLES, String.join(",", payload.roles()))
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPermitPath(String path) {
        return securityProperties.getPermitPaths().stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("X-Error", message);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().add("X-Error", message);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // run before routing
    }
}
