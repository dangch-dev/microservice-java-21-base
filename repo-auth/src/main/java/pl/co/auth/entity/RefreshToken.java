package pl.co.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash("refresh_tokens")
public class RefreshToken implements Serializable {
    @Id
    private String id; // jti

    @Indexed
    private String userId;

    @Indexed
    private String token;

    private String parentJti;

    private Instant expiresAt;

    private boolean revoked;

    @TimeToLive
    private Long ttlSeconds;
}

