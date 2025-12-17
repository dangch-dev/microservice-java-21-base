package pl.co.common.exception;

public class ForbiddenException extends ApiException {
    private static final ErrorCode DEFAULT = ErrorCode.E240;

    public ForbiddenException(String message) {
        super(DEFAULT, message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(DEFAULT, message, cause);
    }
}
