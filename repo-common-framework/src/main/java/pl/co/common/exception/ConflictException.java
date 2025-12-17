package pl.co.common.exception;

public class ConflictException extends ApiException {
    private static final ErrorCode DEFAULT = ErrorCode.E220;

    public ConflictException(String message) {
        super(DEFAULT, message);
    }

    public ConflictException(String message, Throwable cause) {
        super(DEFAULT, message, cause);
    }
}
