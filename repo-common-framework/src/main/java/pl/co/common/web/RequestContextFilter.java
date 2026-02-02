package pl.co.common.web;

import pl.co.common.security.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class RequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = request.getHeader(SecurityConstants.HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        String sessionId = request.getHeader(SecurityConstants.HEADER_SESSION_ID);
        MDC.put("requestId", requestId);
        if (sessionId != null && !sessionId.isBlank()) {
            MDC.put("sessionId", sessionId);
            response.setHeader(SecurityConstants.HEADER_SESSION_ID, sessionId);
        }
        response.setHeader(SecurityConstants.HEADER_REQUEST_ID, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            MDC.remove("sessionId");
        }
    }
}
