package pl.co.common.exception;

public class ExternalServiceException extends ApiException {
    private static final ErrorCode DEFAULT = ErrorCode.E281;

    public ExternalServiceException(String message) {
        super(DEFAULT, message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(DEFAULT, message, cause);
    }
}
