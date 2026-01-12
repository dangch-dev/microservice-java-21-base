package pl.co.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import pl.co.common.dto.ApiResponse;
import pl.co.common.exception.ErrorCode;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthenticationEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(ErrorCode.UNAUTHORIZED.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.message());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
