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
@RedisHash("email_verification_otp")
public class EmailVerificationOTP implements Serializable {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String otpHash;

    private Instant expiresAt;

    private int attempts;

    @TimeToLive
    private Long ttlSeconds;
}
