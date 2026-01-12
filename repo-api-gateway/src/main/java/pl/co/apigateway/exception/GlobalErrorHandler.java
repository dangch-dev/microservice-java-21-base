package pl.co.apigateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ErrorCode;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        ErrorCode errorCode = resolveErrorCode(status, ex);
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = status.getReasonPhrase();
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Void> body = ApiResponse.error(errorCode.code(), message);
        final String errorCodeStr = errorCode.code();
        final String messageStr = message;
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(body))
                .onErrorResume(writeEx -> {
                    byte[] fallback = ("{\"success\":false,\"errorCode\":\"" + errorCodeStr + "\",\"errorMessage\":\"" + messageStr + "\"}")
                            .getBytes(StandardCharsets.UTF_8);
                    return Mono.just(fallback);
                })
                .flatMap(bytes -> response.writeWith(Mono.just(response.bufferFactory().wrap(bytes))));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.resolve(rse.getStatusCode().value());
        }
        if (ex instanceof TimeoutException || ex instanceof java.util.concurrent.CancellationException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        // fallback for lb no instance, etc.
        return HttpStatus.SERVICE_UNAVAILABLE;
    }

    private ErrorCode resolveErrorCode(HttpStatus status, Throwable ex) {
        return switch (status) {
            case GATEWAY_TIMEOUT -> ErrorCode.DEPENDENCY_TIMEOUT;
            case SERVICE_UNAVAILABLE -> ErrorCode.SERVICE_UNAVAILABLE;
            case BAD_GATEWAY -> ErrorCode.BAD_REQUEST; // not perfect, but avoid null
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}
