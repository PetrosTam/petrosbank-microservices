package com.backendev.accountservice.integration.service;

import com.backendev.accountservice.service.SecurityService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceIT {

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGetCurrentUserId() {
        String userId = "user-123";
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId, null, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String result = securityService.getCurrentUserId();

        assertThat(result).isEqualTo(userId);
    }

    @Test
    void shouldThrowException_WhenNoAuthenticationFound() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> securityService.getCurrentUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user found");
    }

    @Test
    void shouldThrowException_WhenAuthenticationNotAuthenticated() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", null, new ArrayList<>());
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> securityService.getCurrentUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user found");
    }

    @Test
    void shouldGet_CurrentUserEmail() {
        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("john@example.com");

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getDetails()).thenReturn(claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String result = securityService.getCurrentUserEmail();

        assertThat(result).isEqualTo("john@example.com");
    }

    @Test
    void shouldReturnNull_IfEmailIsInvalid() {
        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("invalid-email");

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getDetails()).thenReturn(claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String result = securityService.getCurrentUserEmail();

        assertThat(result).isNull();
    }

    @Test
    void shouldThrowException_WhenClaimsNotFound() {
        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getDetails()).thenReturn("not-claims");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(() -> securityService.getCurrentUserEmail())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT claims not found in authentication details");
    }

    @Test
    void shouldGetCurrentUserRoles() {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        List<String> roles = securityService.getCurrentUserRoles();

        assertThat(roles).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void shouldReturn_EmptyRoles_WhenNoAuthorities() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("user", null, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        List<String> roles = securityService.getCurrentUserRoles();

        assertThat(roles).isEmpty();
    }

    @Test
    void shouldThrowException_WhenGetRolesAndNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> securityService.getCurrentUserRoles())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user found");
    }

}
