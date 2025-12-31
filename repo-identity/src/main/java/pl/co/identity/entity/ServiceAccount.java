package pl.co.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.co.common.jpa.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "service_account", indexes = {
        @Index(name = "uk_service_account_client_id", columnList = "client_id", unique = true)
})
public class ServiceAccount extends BaseEntity {

    @Column(name = "service_name", length = 128, nullable = false)
    private String serviceName;

    @Column(name = "client_id", length = 128, nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_secret_hash", length = 255, nullable = false)
    private String clientSecretHash;

    @Column(name = "scopes", length = 255, nullable = false)
    private String scopes;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @PrePersist
    public void applyDefaults() {
        if (enabled == null) {
            enabled = true;
        }
    }
}
