package pl.co.common.exception;

public class UnauthorizedException extends ApiException {
    private static final ErrorCode DEFAULT = ErrorCode.E238;

    public UnauthorizedException(String message) {
        super(DEFAULT, message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(DEFAULT, message, cause);
    }
}
