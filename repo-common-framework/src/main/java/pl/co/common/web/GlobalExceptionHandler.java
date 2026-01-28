package pl.co.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ErrorCode code = ex.getErrorCode() == null ? ErrorCode.INTERNAL_ERROR : ex.getErrorCode();
        HttpStatus status = code.status();
        ApiResponse<Void> body = ApiResponse.error(code.code(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        boolean missingParam = ex.getBindingResult().getAllErrors().stream()
                .anyMatch(err -> {
                    String[] codes = err.getCodes();
                    if (codes == null) {
                        return false;
                    }
                    for (String code : codes) {
                        if (code != null && (code.contains("NotBlank") || code.contains("NotNull") || code.contains("NotEmpty"))) {
                            return true;
                        }
                    }
                    return false;
                });
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(err -> err instanceof FieldError fe ? fe.getField() + " " + fe.getDefaultMessage() : err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ErrorCode code = missingParam ? ErrorCode.E243 : ErrorCode.BAD_REQUEST;
        ApiResponse<Void> body = ApiResponse.error(code.code(), message);
        return ResponseEntity.status(code.status()).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        boolean missingParam = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getConstraintDescriptor)
                .map(descriptor -> descriptor.getAnnotation().annotationType().getSimpleName())
                .anyMatch(name -> "NotBlank".equals(name) || "NotNull".equals(name) || "NotEmpty".equals(name));
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        ErrorCode code = missingParam ? ErrorCode.E243 : ErrorCode.BAD_REQUEST;
        ApiResponse<Void> body = ApiResponse.error(code.code(), message);
        return ResponseEntity.status(code.status()).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = ErrorCode.E202.message();
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormat) {
            String path = invalidFormat.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining("."));
            if (!path.isBlank()) {
                message = "Invalid data type: " + path;
            }
        }
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.E202.code(), message);
        return ResponseEntity.status(ErrorCode.E202.status()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.FORBIDDEN.code(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.METHOD_NOT_ALLOW.code(), ex.getMessage());
        return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOW.status()).body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        String name = ex.getParameterName();
        String message = name == null || name.isBlank()
                ? ErrorCode.E243.message()
                : "Required parameter missing: " + name;
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.E243.code(), message);
        return ResponseEntity.status(ErrorCode.E243.status()).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName();
        String message = name == null || name.isBlank()
                ? ErrorCode.E221.message("Invalid parameter")
                : ErrorCode.E221.message(name + " is invalid");
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.E221.code(), message);
        return ResponseEntity.status(ErrorCode.E221.status()).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        if (isNotFoundException(ex)) {
            ApiResponse<Void> body = ApiResponse.error(ErrorCode.NOT_FOUND.code(), ErrorCode.NOT_FOUND.message());
            return ResponseEntity.status(ErrorCode.NOT_FOUND.status()).body(body);
        }
        log.error("Unhandled exception", ex);
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(), "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }



    private boolean isNotFoundException(Exception ex) {
        String name = ex.getClass().getName();
        return "org.springframework.web.servlet.NoHandlerFoundException".equals(name)
                || "org.springframework.web.servlet.resource.NoResourceFoundException".equals(name);
    }
}
