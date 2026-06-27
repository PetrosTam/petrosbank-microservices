package com.backendev.accountservice.unit.jwt;

import com.backendev.accountservice.jwt.JwtAuthenticationEntryPoint;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationEntryPointTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private AuthenticationException authException;

    private JwtAuthenticationEntryPoint entryPoint;
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() {
        entryPoint = new JwtAuthenticationEntryPoint();
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
    }

    @Test
    void shouldSetUnauthorizedStatusAndJsonContentType() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(authException.getMessage()).thenReturn("Token expired");
        when(response.getWriter()).thenReturn(printWriter);

        entryPoint.commence(request, response, authException);
        
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
    }

    @Test
    void shouldWriteCorrectJsonErrorResponse() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(authException.getMessage()).thenReturn("Invalid token");
        when(response.getWriter()).thenReturn(printWriter);

        entryPoint.commence(request, response, authException);

        String expectedJson = "{\"error\":\"Unauthorized\",\"message\":\"Invalid token\",\"path\":\"/api/users\"}";
        assertThat(stringWriter.toString()).hasToString(expectedJson);
    }

    @Test
    void shouldHandleNullExceptionMessage() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/orders");
        when(authException.getMessage()).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);
        
        entryPoint.commence(request, response, authException);

        String expectedJson = "{\"error\":\"Unauthorized\",\"message\":\"null\",\"path\":\"/api/orders\"}";
        assertThat(stringWriter.toString()).hasToString(expectedJson);
    }

    @Test
    void shouldHandleSpecialCharactersInPath() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users/test@example.com");
        when(authException.getMessage()).thenReturn("Access denied");
        when(response.getWriter()).thenReturn(printWriter);
        
        entryPoint.commence(request, response, authException);
        
        String expectedJson = "{\"error\":\"Unauthorized\",\"message\":\"Access denied\",\"path\":\"/api/users/test@example.com\"}";
        assertThat(stringWriter.toString()).hasToString(expectedJson);
    }

}