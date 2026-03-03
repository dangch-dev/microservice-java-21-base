package pl.co.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@RedisHash("oauth_callback_state")
public class OAuthCallbackState implements Serializable {
    @Id
    private String id; // state

    private String callback;
    private String mode;
    private String userId;

    @TimeToLive
    private Long ttlSeconds;
}
