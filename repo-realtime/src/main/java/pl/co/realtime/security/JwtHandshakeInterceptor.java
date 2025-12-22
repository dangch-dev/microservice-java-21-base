package pl.co.realtime.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import pl.co.common.exception.ApiException;
import pl.co.common.security.JwtUtils;
import pl.co.common.security.SecurityConstants;

import java.security.Principal;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final RSAPublicKey jwtPublicKey;

    public static final String ATTR_PRINCIPAL_NAME = "wsPrincipalName";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (token == null) {
            // Allow anonymous handshake; principal will become anon-<uuid>
            return true;
        }
        // If token is present, require it to be valid and attach userId as principal
        JwtUtils.JwtPayload payload = JwtUtils.verify(token, jwtPublicKey);
        attributes.put(ATTR_PRINCIPAL_NAME, payload.userId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String resolveToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(SecurityConstants.HEADER_BEARER_PREFIX)) {
            return header.substring(SecurityConstants.HEADER_BEARER_PREFIX.length()).trim();
        }
        // fallback: query param access_token
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String part : query.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2 && kv[0].equals("access_token") && !kv[1].isBlank()) {
                    return kv[1];
                }
            }
        }
        return null;
    }

    public HandshakeHandler handshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                              WebSocketHandler wsHandler,
                                              Map<String, Object> attributes) {
                String name = (String) attributes.get(ATTR_PRINCIPAL_NAME);
                if (name == null || name.isBlank()) {
                    name = "anon-" + UUID.randomUUID();
                }
                String finalName = name;
                return () -> finalName;
            }
        };
    }
}
