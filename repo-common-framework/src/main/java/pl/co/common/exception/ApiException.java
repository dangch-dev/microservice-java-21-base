package pl.co.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;
    private final Object data;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.message(), errorCode.status(), null, null);
    }

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, errorCode.status(), null, null);
    }

    public ApiException(ErrorCode errorCode, String message, Object data) {
        this(errorCode, message, errorCode.status(), data, null);
    }

    public ApiException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, errorCode.message(), errorCode.status(), null, cause);
    }

    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, errorCode.status(), null, cause);
    }

    public ApiException(ErrorCode errorCode, HttpStatus status) {
        this(errorCode, errorCode.message(), status, null, null);
    }

    public ApiException(ErrorCode errorCode, String message, HttpStatus status) {
        this(errorCode, message, status, null, null);
    }

    public ApiException(ErrorCode errorCode, String message, HttpStatus status, Object data, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = status;
        this.data = data;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public Object getData() {
        return data;
    }
}
