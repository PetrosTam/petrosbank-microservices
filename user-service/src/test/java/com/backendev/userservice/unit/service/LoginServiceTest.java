package com.backendev.userservice.unit.service;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.dto.AuthRequest;
import com.backendev.userservice.dto.AuthResponse;
import com.backendev.userservice.entity.Roles;
import com.backendev.userservice.entity.Users;
import com.backendev.userservice.security.AppUserDetails;
import com.backendev.userservice.security.jwt.JwtService;
import com.backendev.userservice.service.AuditService;
import com.backendev.userservice.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @Mock
    private Authentication authentication;

    @Mock
    private AppUserDetails appUserDetails;

    @InjectMocks
    private LoginService loginService;

    private AuthRequest authRequest;



    @BeforeEach
    void setUp() {
        String testEmail = "test@example.com";
        String testPassword = "password123";
        Long testUserId = 1L;
        Users userEntity;

        Roles userRole = new Roles(0L,"ROLE_USER");
        userEntity = new Users();
        userEntity.setId(testUserId);
        userEntity.setEmail(testEmail);
        userEntity.setPassword(testPassword);
        userEntity.setRoles(Set.of(userRole));
        appUserDetails = new AppUserDetails(userEntity);

        authentication = mock(Authentication.class);
        authRequest = new AuthRequest(testEmail, testPassword);
    }

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsAreValid() {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(appUserDetails);
        when(jwtService.generateToken(eq(appUserDetails.getUsername()), anyMap())).thenReturn("mocked-jwt-token");
        when(jwtService.extractExpiration("mocked-jwt-token")).thenReturn(Date.from(Instant.now().plusSeconds(3600)));

        AuthResponse response = loginService.login(authRequest);

        assertEquals("mocked-jwt-token", response.getAccessToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(List.of("ROLE_USER"), response.getRoles());
        assertNotNull(response.getExpiration());

        verify(auditService).auditLog(AuditEventType.LOGIN_SUCCESS, "test@example.com", "Login successful");
    }

    @Test
    void login_shouldThrowBadCredentialsException_whenAuthenticationFails() {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> loginService.login(authRequest));

        assertEquals("Invalid credentials", exception.getMessage());
        verify(auditService, never()).auditLog(any(), any(), any());
    }

}