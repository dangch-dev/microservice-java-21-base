package pl.co.common.exception;

public class ValidationException extends ApiException {
    private static final ErrorCode DEFAULT = ErrorCode.E200;

    public ValidationException(String message) {
        super(DEFAULT, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(DEFAULT, message, cause);
    }
}
