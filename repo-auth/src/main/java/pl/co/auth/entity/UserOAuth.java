package pl.co.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.co.common.jpa.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_oauth", indexes = {
        @Index(name = "uk_user_oauth_provider_sub", columnList = "provider,provider_user_id", unique = true),
        @Index(name = "idx_user_oauth_user_id", columnList = "user_id")
})
public class UserOAuth extends BaseEntity {

    @Column(name = "provider", length = 32, nullable = false)
    private String provider;

    @Column(name = "provider_user_id", length = 128, nullable = false)
    private String providerUserId;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "user_id", length = 26, nullable = false)
    private String userId;
}
