package com.backendev.userservice.unit.controller;

import com.backendev.userservice.audit.AuditEventType;
import com.backendev.userservice.controller.UsersController;
import com.backendev.userservice.dto.AuthRequest;
import com.backendev.userservice.dto.AuthResponse;
import com.backendev.userservice.dto.UserDTO;
import com.backendev.userservice.dto.UserProfileDTO;
import com.backendev.userservice.dto.UserRegistrationRequest;
import com.backendev.userservice.dto.UserRegistrationResponse;
import com.backendev.userservice.security.AppUserDetails;
import com.backendev.userservice.service.AuditService;
import com.backendev.userservice.service.LoginService;
import com.backendev.userservice.service.UsersService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    @Mock
    private UsersService usersService;

    @Mock
    private LoginService loginService;

    @Mock
    private AuditService auditService;

    @Mock
    private Authentication authentication;

    @Mock
    private AppUserDetails userDetails;

    @InjectMocks
    private UsersController usersController;

    @Test
    void register_ValidRequest_ShouldReturnCreatedStatus() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        UserRegistrationResponse response = new UserRegistrationResponse();
        response.setEmail("test@example.com");
        response.setFirstName("John");
        response.setLastName("Doe");

        when(usersService.registerUser(any(UserRegistrationRequest.class))).thenReturn(response);

        ResponseEntity<UserRegistrationResponse> result = usersController.register(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        assertEquals("test@example.com", result.getBody().getEmail());
        assertEquals("John", result.getBody().getFirstName());
        assertEquals("Doe", result.getBody().getLastName());

        verify(usersService).registerUser(any(UserRegistrationRequest.class));
        verify(auditService).auditLog(AuditEventType.REGISTRATION, "test@example.com", "User registered");
    }


    @Test
    void register_ServiceThrowsException_ShouldPropagateException() {
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@example.com");

        when(usersService.registerUser(any(UserRegistrationRequest.class)))
                .thenThrow(new RuntimeException("Email already exists"));

        assertThrows(RuntimeException.class, () -> usersController.register(request));

        verify(auditService, never()).auditLog(any(), any(), any());
    }

    @Test
    void login_ValidCredentials_ShouldReturnOkStatus() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        AuthResponse response = new AuthResponse();
        response.setAccessToken("jwt-token");
        response.setEmail("test@example.com");

        when(loginService.login(any(AuthRequest.class))).thenReturn(response);

        ResponseEntity<AuthResponse> result = usersController.login(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        Assertions.assertNotNull(result.getBody());
        assertEquals("jwt-token", result.getBody().getAccessToken());
        assertEquals("test@example.com", result.getBody().getEmail());

        verify(loginService).login(any(AuthRequest.class));
        verify(auditService, never()).auditLog(any(), any(), any());
    }

    @Test
    void login_InvalidCredentials_ShouldReturnUnauthorizedAndAuditFailure() {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        when(loginService.login(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> usersController.login(request));

        assertEquals("Invalid credentials", exception.getMessage());
        verify(loginService).login(any(AuthRequest.class));
        verify(auditService).auditLog(AuditEventType.LOGIN_FAILURE, "", "Invalid credentials");
    }


    @Test
    void getUserProfile_ValidIdAndAuthentication_ShouldReturnUserDTO() {
        Long userId = 1L;
        UserDTO userDTO = new UserDTO();
        userDTO.setFirstName("John");
        userDTO.setLastName("Doe");
        userDTO.setEmail("test@example.com");
        userDTO.setPhone("444555666");

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(userId);
        when(usersService.findUserByID(userId)).thenReturn(userDTO);

        ResponseEntity<UserDTO> response = usersController.getUserProfile(userId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userDTO, response.getBody());
        verify(usersService).findUserByID(userId);
        verify(auditService, never()).auditLog(any(), any(), any());
    }

    @Test
    void getUserProfile_MismatchedUserId_ShouldThrowAccessDeniedExceptionAndAudit() {
        Long requestedUserId = 1L;
        Long authenticatedUserId = 2L;
        String userEmail = "test@example.com";

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(authenticatedUserId);
        when(userDetails.getEmail()).thenReturn(userEmail);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> usersController.getUserProfile(requestedUserId, authentication));

        assertEquals("You can only access your own profile.", exception.getMessage());
        verify(auditService).auditLog(AuditEventType.INVALID_TOKEN, userEmail, "Access denied for the user");
        verify(usersService, never()).findUserByID(any());
    }

    @Test
    void getUserProfile_ServiceThrowsException_ShouldPropagateException() {
        Long userId = 1L;
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(userId);
        when(usersService.findUserByID(userId)).thenThrow(new RuntimeException("User not found"));

        assertThrows(RuntimeException.class, () -> usersController.getUserProfile(userId, authentication));
    }

    @Test
    void updateUserProfile_ValidRequest_ShouldReturnUpdatedUserDTO() {
        Long userId = 1L;
        String userEmail = "test@example.com";

        UserProfileDTO updateRequest = new UserProfileDTO();
        updateRequest.setFirstName("UpdatedName");

        UserDTO updatedUserDTO = new UserDTO();
        updatedUserDTO.setFirstName("UpdatedName");

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(userId);
        when(userDetails.getEmail()).thenReturn(userEmail);
        when(usersService.updateUser(userId, updateRequest)).thenReturn(updatedUserDTO);

        ResponseEntity<UserDTO> response = usersController.updateUserProfile(userId, updateRequest, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedUserDTO, response.getBody());
        verify(usersService).updateUser(userId, updateRequest);
        verify(auditService).auditLog(AuditEventType.PROFILE_UPDATED, userEmail, "User profile updated");
    }

    @Test
    void updateUserProfile_MismatchedUserId_ShouldThrowAccessDeniedExceptionAndAudit() {
        Long requestedUserId = 1L;
        Long authenticatedUserId = 2L;
        String userEmail = "test@example.com";
        UserProfileDTO updateRequest = new UserProfileDTO();

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(authenticatedUserId);
        when(userDetails.getEmail()).thenReturn(userEmail);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> usersController.updateUserProfile(requestedUserId, updateRequest, authentication));

        assertEquals("You can only access your own profile.", exception.getMessage());
        verify(auditService).auditLog(AuditEventType.INVALID_TOKEN, userEmail, "Access denied for the user");
        verify(usersService, never()).updateUser(any(), any());
    }

    @Test
    void updateUserProfile_ServiceThrowsException_ShouldPropagateException() {
        Long userId = 1L;
        UserProfileDTO updateRequest = new UserProfileDTO();

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getId()).thenReturn(userId);
        when(usersService.updateUser(userId, updateRequest))
                .thenThrow(new RuntimeException("Update failed"));

        assertThrows(RuntimeException.class, () -> usersController.updateUserProfile(userId, updateRequest, authentication));

        verify(auditService, never()).auditLog(any(), any(), any());
    }
}