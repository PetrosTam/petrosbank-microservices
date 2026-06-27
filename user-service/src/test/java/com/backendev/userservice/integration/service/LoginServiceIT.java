package com.backendev.userservice.integration.service;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.dto.AuthRequest;
import com.backendev.userservice.dto.AuthResponse;
import com.backendev.userservice.dto.UserRegistrationRequest;
import com.backendev.userservice.entity.Roles;
import com.backendev.userservice.entity.Users;
import com.backendev.userservice.repository.AuditLogRepository;
import com.backendev.userservice.repository.UsersRepository;
import com.backendev.userservice.security.AppUserDetails;
import com.backendev.userservice.security.jwt.JwtService;
import com.backendev.userservice.service.AuditService;
import com.backendev.userservice.service.LoginService;
import com.backendev.userservice.service.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LoginServiceIT {

    @Mock
    private AuthenticationManager authenticationManager;

    private LoginService loginService;

    @Autowired
    private UsersService usersService;

    @Autowired
    private UsersRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        auditLogRepository.deleteAll();
        reset(authenticationManager);
        loginService = new LoginService(authenticationManager, jwtService, auditService);
    }

    @Test
    void testLogin_Success() {
        UserRegistrationRequest regRequest = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();
        usersService.registerUser(regRequest);

        Roles userRole = new Roles();
        userRole.setName("ROLE_USER");

        Users mockUser = new Users();
        mockUser.setId(1L);
        mockUser.setEmail("john@example.com");
        mockUser.setPassword("password123");
        mockUser.setRoles(Set.of(userRole));

        AppUserDetails userDetails = new AppUserDetails(mockUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        AuthRequest loginRequest = new AuthRequest("john@example.com", "password123");

        AuthResponse response = loginService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRoles()).contains("ROLE_USER");
        assertThat(response.getExpiration()).isNotNull();

        var auditLogs = auditLogRepository.findAll();
        assertThat(auditLogs)
                .isNotEmpty()
                .anyMatch(log -> log.getAuditEventType() == AuditEventType.LOGIN_SUCCESS &&
                        log.getEmail().equals("john@example.com")
        );    }


    @Test
    void testLogin_InvalidCredentials() {
        AuthRequest loginRequest = new AuthRequest("john@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> loginService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");
    }

    @Test
    void testLogin_IncludesAllRolesInResponse() {
        Roles adminRole = new Roles();
        adminRole.setName("ROLE_ADMIN");

        Roles userRole = new Roles();
        userRole.setName("ROLE_USER");

        Users mockUser = new Users();
        mockUser.setId(1L);
        mockUser.setEmail("john@example.com");
        mockUser.setPassword("password123");
        mockUser.setRoles(Set.of(adminRole, userRole));

        AppUserDetails userDetails = new AppUserDetails(mockUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        AuthRequest loginRequest = new AuthRequest("john@example.com", "password123");

        AuthResponse response = loginService.login(loginRequest);

        assertThat(response.getRoles())
                .contains("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void testLogin_AuthenticationManagerCalled() {
        Roles userRole = new Roles();
        userRole.setName("ROLE_USER");

        Users mockUser = new Users();
        mockUser.setId(1L);
        mockUser.setEmail("john@example.com");
        mockUser.setPassword("password123");
        mockUser.setRoles(Set.of(userRole));

        AppUserDetails userDetails = new AppUserDetails(mockUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        AuthRequest loginRequest = new AuthRequest("john@example.com", "password123");

        loginService.login(loginRequest);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void testLogin_NotAuthenticatedThrowsException() {
        // Mock authentication that returns false for isAuthenticated()
        Authentication failedAuth = new UsernamePasswordAuthenticationToken("john@example.com", "password123");
        failedAuth.setAuthenticated(false);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(failedAuth);

        AuthRequest loginRequest = new AuthRequest("john@example.com", "password123");

        assertThatThrownBy(() -> loginService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void testLogin_ExpirationDateNotNull() {
        Roles userRole = new Roles();
        userRole.setName("ROLE_USER");

        Users mockUser = new Users();
        mockUser.setId(1L);
        mockUser.setEmail("john@example.com");
        mockUser.setPassword("password123");
        mockUser.setRoles(Set.of(userRole));

        AppUserDetails userDetails = new AppUserDetails(mockUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        AuthRequest loginRequest = new AuthRequest("john@example.com", "password123");

        AuthResponse response = loginService.login(loginRequest);

        assertThat(response.getExpiration()).isNotNull();
        assertThat(response.getExpiration().getTime()).isGreaterThan(System.currentTimeMillis());
    }
}
