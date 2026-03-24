package pl.co.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash("google_oauth_tokens")
public class GoogleOAuthToken implements Serializable {
    @Id
    private String id;

    @Indexed
    private String userId;

    private String googleSubject;
    private String googleEmail;
    private String googleName;
    private String googleAvatarUrl;

    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
}
