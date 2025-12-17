package pl.co.common.exception;

public class TokenInvalidException extends ApiException {
    private static final ErrorCode DEFAULT = ErrorCode.E241;

    public TokenInvalidException(String message) {
        super(DEFAULT, message);
    }

    public TokenInvalidException(String message, Throwable cause) {
        super(DEFAULT, message, cause);
    }
}
