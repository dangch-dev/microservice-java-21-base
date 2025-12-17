package pl.co.common.exception;

public class NotFoundException extends ApiException {
    private static final ErrorCode DEFAULT = ErrorCode.E227;

    public NotFoundException(String message) {
        super(DEFAULT, message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(DEFAULT, message, cause);
    }
}
