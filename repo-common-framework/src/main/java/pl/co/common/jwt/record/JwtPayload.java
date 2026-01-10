package pl.co.common.jwt.record;

import java.util.Set;

public record JwtPayload(String jti,
                         String userId,
                         Set<String> roles,
                         boolean emailVerified,
                         String type,
                         String parentJti) {
}
