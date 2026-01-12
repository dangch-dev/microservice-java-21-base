package pl.co.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ErrorCode;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccessDeniedHandler implements org.springframework.security.web.access.AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
