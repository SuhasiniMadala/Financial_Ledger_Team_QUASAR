package com.quasar.ledger.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.web.ErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Writes 401 UNAUTHENTICATED and 403 FORBIDDEN in the standard error format (SRS Table 3-2). */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ErrorResponseFactory errors;
    private final ObjectMapper json;

    public SecurityErrorHandler(ErrorResponseFactory errors, ObjectMapper json) {
        this.errors = errors;
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        write(response, request, ErrorCode.UNAUTHENTICATED, "Missing, invalid or expired access token");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException {
        write(response, request, ErrorCode.FORBIDDEN, "Your role is not allowed to use this endpoint");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, ErrorCode code, String message) throws IOException {
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), errors.create(code, message, request));
    }
}
