package pl.co.common.exception;

public class RateLimitException extends ApiException {
    private static final ErrorCode DEFAULT = ErrorCode.E650;

    public RateLimitException(String message) {
        super(DEFAULT, message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(DEFAULT, message, cause);
    }
}
