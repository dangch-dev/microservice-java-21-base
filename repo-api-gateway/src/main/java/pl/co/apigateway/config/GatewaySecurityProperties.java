package pl.co.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "security.jwt")
public class GatewaySecurityProperties {
    /**
     * HS256 shared secret for JWT validation.
     */
    private String secret;

    /**
     * Require authentication by default.
     */
    private boolean required = true;

    /**
     * Ant-style path patterns that are allowed without authentication.
     */
    private List<String> permitPaths = new ArrayList<>();

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<String> getPermitPaths() {
        return permitPaths;
    }

    public void setPermitPaths(List<String> permitPaths) {
        this.permitPaths = permitPaths;
    }
}
