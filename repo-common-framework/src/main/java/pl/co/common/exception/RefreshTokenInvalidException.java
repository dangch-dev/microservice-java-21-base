package pl.co.common.exception;

public class RefreshTokenInvalidException extends ApiException {
    private static final ErrorCode DEFAULT = ErrorCode.E248;

    public RefreshTokenInvalidException(String message) {
        super(DEFAULT, message);
    }

    public RefreshTokenInvalidException(String message, Throwable cause) {
        super(DEFAULT, message, cause);
    }
}
