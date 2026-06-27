package com.backendev.userservice.unit.jwt;

import com.backendev.userservice.security.AppUserServiceDetails;
import com.backendev.userservice.security.jwt.JwtFilter;
import com.backendev.userservice.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private AppUserServiceDetails userServiceDetails;

    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtService, userServiceDetails);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        // Reset before each test
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBypassJwtProcessingForSwaggerPath() throws ServletException, IOException {
        request.setRequestURI("/swagger-ui/index.html");

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotAuthenticateWhenNoAuthorizationHeader() throws ServletException, IOException {
        request.setRequestURI("/api/protected");

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotAuthenticateWhenAuthorizationHeaderIsMalformed() throws ServletException, IOException {
        request.setRequestURI("/api/protected");
        request.addHeader("Authorization", "Basic abc123");

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldAuthenticateUserWithValidToken() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String username = "user@example.com";

        request.setRequestURI("/api/protected");
        request.addHeader("Authorization", "Bearer " + token);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                username,
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        when(jwtService.extractUsername(token)).thenReturn(username);
        when(userServiceDetails.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtService.validateToken(token)).thenReturn(true);

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void shouldNotAuthenticateWhenBearerTokenIsEmpty() throws ServletException, IOException {
        request.setRequestURI("/api/protected");
        request.addHeader("Authorization", "Bearer ");

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSkipAuthenticationIfAlreadyAuthenticated() throws ServletException, IOException {
        request.setRequestURI("/api/protected");
        request.addHeader("Authorization", "Bearer valid.jwt.token");

        Authentication existingAuth = new UsernamePasswordAuthenticationToken("user", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals("user", SecurityContextHolder.getContext().getAuthentication().getName());
    }

}