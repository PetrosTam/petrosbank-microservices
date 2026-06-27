package com.backendev.accountservice.unit.jwt;

import com.backendev.accountservice.exception.JwtAuthenticationException;
import com.backendev.accountservice.jwt.JwtAuthenticationFilter;
import com.backendev.accountservice.jwt.JwtTokenValidator;
import com.backendev.accountservice.jwt.PublicEndpointMatcher;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenValidator tokenValidator;

    @Mock
    private PublicEndpointMatcher publicEndpointMatcher;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    private JwtAuthenticationFilter filter;
    private StringWriter stringWriter;

    @BeforeEach
    void setUp(){
        filter = new JwtAuthenticationFilter(tokenValidator, publicEndpointMatcher){};
        stringWriter = new StringWriter();
        SecurityContextHolder.clearContext();
    }


    @Test
    void shouldSkipAuthenticationForPublicEndpoints() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/public");
        when(request.getMethod()).thenReturn("GET");
        when(publicEndpointMatcher.isPublicEndpoint("/api/public")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tokenValidator, response);
    }

    @Test
    void shouldAuthenticateValidToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(publicEndpointMatcher.isPublicEndpoint("/api/users")).thenReturn(false);
        when(tokenValidator.validateTokenAndCreateAuthentication("valid-token")).thenReturn(authentication);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    void shouldRejectInvalidToken() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
        when(publicEndpointMatcher.isPublicEndpoint("/api/users")).thenReturn(false);
        when(tokenValidator.validateTokenAndCreateAuthentication("invalid"))
                .thenThrow(new JwtAuthenticationException("Invalid"));
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        filter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
    }
}