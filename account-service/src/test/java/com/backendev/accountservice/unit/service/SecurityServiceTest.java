package com.backendev.accountservice.unit.service;

import com.backendev.accountservice.service.SecurityService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private Claims claims;

    @InjectMocks
    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class GetCurrentUserIdTests {

        @Test
        void shouldReturnUserId_whenAuthenticated() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("user123");

            String result = securityService.getCurrentUserId();

            assertEquals("user123", result);
        }

        @Test
        void shouldThrowException_whenNoAuthentication() {
            when(securityContext.getAuthentication()).thenReturn(null);

            assertThrows(IllegalStateException.class, () -> securityService.getCurrentUserId());
        }

        @Test
        void shouldThrowException_whenNotAuthenticated() {
            when(authentication.isAuthenticated()).thenReturn(false);

            assertThrows(IllegalStateException.class, () -> securityService.getCurrentUserId());
        }
    }

    @Nested
    class GetCurrentUserEmailTests {

        @Test
        void shouldReturnEmail_whenValidEmailInClaims() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getDetails()).thenReturn(claims);
            when(claims.getSubject()).thenReturn("user@example.com");

            String result = securityService.getCurrentUserEmail();

            assertEquals("user@example.com", result);
        }

        @Test
        void shouldReturnNull_whenSubjectIsNotEmail() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getDetails()).thenReturn(claims);
            when(claims.getSubject()).thenReturn("user123");

            String result = securityService.getCurrentUserEmail();

            assertNull(result);
        }

        @Test
        void shouldReturnNull_whenSubjectIsNull() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getDetails()).thenReturn(claims);
            when(claims.getSubject()).thenReturn(null);

            String result = securityService.getCurrentUserEmail();

            assertNull(result);
        }

        @Test
        void shouldThrowException_whenDetailsNotClaims() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getDetails()).thenReturn("not-claims");

            assertThrows(IllegalStateException.class, () -> securityService.getCurrentUserEmail());
        }

        @Test
        void shouldThrowException_whenNoAuthentication() {
            when(securityContext.getAuthentication()).thenReturn(null);

            assertThrows(IllegalStateException.class, () -> securityService.getCurrentUserEmail());
        }

        @Test
        void shouldThrowException_whenNotAuthenticated() {
            when(authentication.isAuthenticated()).thenReturn(false);

            assertThrows(IllegalStateException.class, () -> securityService.getCurrentUserEmail());
        }
    }

    @Nested
    class GetCurrentUserRolesTests {

        @Test
        void shouldReturnRoles_whenAuthenticated() {
            Collection<GrantedAuthority> authorities = Arrays.asList(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
            when(authentication.isAuthenticated()).thenReturn(true);
            doReturn(authorities).when(authentication).getAuthorities();

            List<String> result = securityService.getCurrentUserRoles();

            assertEquals(List.of("ROLE_USER", "ROLE_ADMIN"), result);
        }

        @Test
        void shouldReturnEmptyList_whenNoAuthorities() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getAuthorities()).thenReturn(Collections.emptyList());

            List<String> result = securityService.getCurrentUserRoles();

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowException_whenNoAuthentication() {
            when(securityContext.getAuthentication()).thenReturn(null);

            assertThrows(IllegalStateException.class, () -> securityService.getCurrentUserRoles());
        }

        @Test
        void shouldThrowException_whenNotAuthenticated() {
            when(authentication.isAuthenticated()).thenReturn(false);

            assertThrows(IllegalStateException.class, () -> securityService.getCurrentUserRoles());
        }
    }
}