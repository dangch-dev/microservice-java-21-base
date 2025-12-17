package pl.co.common.web;

import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ApiException;
import pl.co.common.exception.ErrorCode;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ErrorCode code = ex.getErrorCode();
        ApiResponse<Void> body = ApiResponse.error(code.code(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors();
        List<String> missingFields = errors.stream()
                .filter(fe -> fe.getDefaultMessage() != null && fe.getDefaultMessage().startsWith("Required parameter is missing value"))
                .map(FieldError::getField)
                .toList();
        List<String> otherMessages = errors.stream()
                .filter(fe -> fe.getDefaultMessage() == null || !fe.getDefaultMessage().startsWith("Required parameter is missing value"))
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        List<String> parts = new ArrayList<>();
        if (!missingFields.isEmpty()) {
            parts.add("Required parameter is missing value. (" + String.join(",", missingFields) + ")");
        }
        otherMessages.stream().filter(msg -> msg != null && !msg.isBlank()).forEach(parts::add);

        String message = String.join(", ", parts);
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.E200.code(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.INTERNAL_ERROR.code(), "Internal error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String currentTraceId() {
        return MDC.get("requestId");
    }
}
