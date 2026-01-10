package pl.co.common.filter.principal;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Set;

public record AuthPrincipal(String userId, boolean emailVerified, Set<String> roles)
        implements Principal, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return userId;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(Collection<String> candidate) {
        return roles != null && candidate.stream().anyMatch(roles::contains);
    }
}
