package pl.co.common.filter.principal;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.Set;

public record ServicePrincipal(String serviceName, Set<String> scopes) implements Principal, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return serviceName;
    }
}
