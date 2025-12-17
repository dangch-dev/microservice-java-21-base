package pl.co.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Validation / Input
    E200("200", HttpStatus.BAD_REQUEST), // Input Parameter Error (generic)
    E201("201", HttpStatus.BAD_REQUEST), // Invalid data length
    E202("202", HttpStatus.BAD_REQUEST), // Invalid data type
    E203("203", HttpStatus.BAD_REQUEST), // Invalid data format
    E204("204", HttpStatus.BAD_REQUEST), // Invalid data value
    E243("243", HttpStatus.BAD_REQUEST), // Required parameter missing
    E215("215", HttpStatus.BAD_REQUEST), // Invalid file size
    E216("216", HttpStatus.BAD_REQUEST), // Invalid image/file type
    E217("217", HttpStatus.BAD_REQUEST), // Invalid image width
    E218("218", HttpStatus.BAD_REQUEST), // Invalid image height
    E219("219", HttpStatus.BAD_REQUEST), // File count exceeded
    E244("244", HttpStatus.BAD_REQUEST), // Invalid phone number
    E245("245", HttpStatus.BAD_REQUEST), // Invalid email address
    E252("252", HttpStatus.BAD_REQUEST), // Invalid password complexity
    E247("247", HttpStatus.BAD_REQUEST), // Invalid date of birth / age
    E221("221", HttpStatus.BAD_REQUEST), // Requested data invalid
    E228("228", HttpStatus.BAD_REQUEST), // Registration count exceeded
    E225("225", HttpStatus.BAD_REQUEST), // Data more than expected

    // Auth / Token / Permission
    E238("238", HttpStatus.UNAUTHORIZED), // Username does not exist
    E239("239", HttpStatus.UNAUTHORIZED), // Password incorrect
    E234("234", HttpStatus.UNAUTHORIZED), // Authentication expired
    E241("241", HttpStatus.UNAUTHORIZED), // Token invalid
    E248("248", HttpStatus.UNAUTHORIZED), // Invalid refreshToken
    E602("602", HttpStatus.UNAUTHORIZED), // OTP authentication failed
    E242("242", HttpStatus.FORBIDDEN),    // OTP expired
    E230("230", HttpStatus.FORBIDDEN),    // No authority
    E240("240", HttpStatus.FORBIDDEN),    // No permission
    E233("233", HttpStatus.FORBIDDEN),    // Email not verified

    // Conflict / Duplicate / State
    E220("220", HttpStatus.CONFLICT), // Already registered / duplicate
    E255("255", HttpStatus.CONFLICT), // User exists (email)
    E256("256", HttpStatus.CONFLICT), // User exists (phone)
    E250("250", HttpStatus.CONFLICT), // Another request in progress

    // Not found / Data integrity
    E223("223", HttpStatus.NOT_FOUND), // No master data
    E227("227", HttpStatus.NOT_FOUND), // No data found

    // System / External
    E235("235", HttpStatus.SERVICE_UNAVAILABLE), // Service not available
    E280("280", HttpStatus.INTERNAL_SERVER_ERROR), // Server error
    E281("281", HttpStatus.SERVICE_UNAVAILABLE), // External API error
    E305("305", HttpStatus.BAD_GATEWAY), // Internal API call error
    E282("282", HttpStatus.SERVICE_UNAVAILABLE), // Failed sending SMS
    E650("650", HttpStatus.TOO_MANY_REQUESTS), // Retry limit reached

    // Generic fallbacks
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN),
    NOT_FOUND("NOT_FOUND", HttpStatus.NOT_FOUND),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT),
    TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", HttpStatus.TOO_MANY_REQUESTS),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE),
    DEPENDENCY_TIMEOUT("DEPENDENCY_TIMEOUT", HttpStatus.GATEWAY_TIMEOUT);

    private final String code;
    private final HttpStatus status;

    ErrorCode(String code, HttpStatus status) {
        this.code = code;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }
}
