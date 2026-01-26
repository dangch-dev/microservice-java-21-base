package pl.co.common.security;

import org.springframework.security.core.Authentication;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import pl.co.common.filter.principal.AuthPrincipal;

public final class AuthUtils {

    private AuthUtils() {
    }

    public static String resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }
        if (authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return principal.userId();
        }
        throw new ApiException(ErrorCode.UNAUTHORIZED);
    }

}
