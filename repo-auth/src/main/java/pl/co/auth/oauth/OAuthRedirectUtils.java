package pl.co.auth.oauth;

import pl.co.common.util.StringUtils;

final class OAuthRedirectUtils {

    private OAuthRedirectUtils() {
    }

    static String buildRedirectUrl(String callback, String frontendBaseUrl) {
        if (!StringUtils.hasText(callback)) {
            return null;
        }
        if (isAbsoluteUrl(callback)) {
            return isAuthorizedRedirectUri(callback, frontendBaseUrl) ? callback : null;
        }
        String baseOrigin = resolveBaseOrigin(frontendBaseUrl);
        if (!StringUtils.hasText(baseOrigin)) {
            return null;
        }
        String path = callback.startsWith("/") ? callback : "/" + callback;
        return baseOrigin + path;
    }

    static String appendError(String targetUrl, String code) {
        String separator = targetUrl.contains("?") ? "&" : "?";
        return targetUrl + separator + "oauth_error=" + code;
    }

    private static boolean isAuthorizedRedirectUri(String uri, String frontendBaseUrl) {
        if (!StringUtils.hasText(frontendBaseUrl)) {
            return false;
        }
        try {
            java.net.URI client = java.net.URI.create(uri);
            java.net.URI base = java.net.URI.create(frontendBaseUrl);
            return sameOrigin(client, base);
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean sameOrigin(java.net.URI a, java.net.URI b) {
        if (a == null || b == null) {
            return false;
        }
        String schemeA = a.getScheme();
        String schemeB = b.getScheme();
        String hostA = a.getHost();
        String hostB = b.getHost();
        int portA = normalizedPort(a);
        int portB = normalizedPort(b);
        return StringUtils.hasText(schemeA) && StringUtils.hasText(schemeB)
                && StringUtils.hasText(hostA) && StringUtils.hasText(hostB)
                && schemeA.equalsIgnoreCase(schemeB)
                && hostA.equalsIgnoreCase(hostB)
                && portA == portB;
    }

    private static int normalizedPort(java.net.URI uri) {
        int port = uri.getPort();
        if (port != -1) {
            return port;
        }
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return 80;
    }

    private static boolean isAbsoluteUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private static String resolveBaseOrigin(String frontendBaseUrl) {
        if (!StringUtils.hasText(frontendBaseUrl)) {
            return null;
        }
        try {
            java.net.URI base = java.net.URI.create(frontendBaseUrl);
            if (!StringUtils.hasText(base.getScheme()) || !StringUtils.hasText(base.getHost())) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(base.getScheme()).append("://").append(base.getHost());
            if (base.getPort() != -1) {
                sb.append(":").append(base.getPort());
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }
}
