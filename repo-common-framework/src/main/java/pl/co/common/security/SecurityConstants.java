package pl.co.common.security;

public final class SecurityConstants {
    private SecurityConstants() {
    }

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_BEARER_PREFIX = "Bearer ";
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_EMAIL_VERIFIED = "email_verified";
    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_PARENT_JTI = "prt";

    public static final String TYP_ACCESS = "access";
    public static final String TYP_REFRESH = "refresh";
    public static final String AUD_EXTERNAL = "external";
    public static final String AUD_INTERNAL = "internal";
}
