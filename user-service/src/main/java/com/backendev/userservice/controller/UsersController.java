package com.backendev.userservice.controller;

import com.backendev.userservice.audit.AuditEventType;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/users")
public class UsersController {

    private final UsersService usersService;
    private final LoginService loginService;
    private final AuditService auditService;

    public UsersController(UsersService usersService, LoginService loginService, AuditService auditService) {
        this.usersService = usersService;
        this.loginService = loginService;
        this.auditService = auditService;
    }

    private static final Logger log = LoggerFactory.getLogger(UsersController.class);

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> register(@Valid @RequestBody UserRegistrationRequest userRegistrationRequest){
        log.info("Registration for new user request {}", userRegistrationRequest);
        UserRegistrationResponse savedUser = usersService.registerUser(userRegistrationRequest);
        auditService.auditLog(AuditEventType.REGISTRATION, savedUser.getEmail(), "User registered");
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest){
        log.info("Login for request of user {}", authRequest.getEmail());
        try {
            AuthResponse authResponse = loginService.login(authRequest);
            log.debug("Login successful. JWT issued");
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
        } catch (BadCredentialsException exception) {
            log.error("Login failed for user {}", authRequest.getEmail());
            auditService.auditLog(AuditEventType.LOGIN_FAILURE, "", "Invalid credentials");
            throw exception;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable Long id, Authentication authentication) {
        log.info("Get user profile request for the id {}", id);
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        if (!userDetails.getId().equals(id)) {
            auditService.auditLog(AuditEventType.INVALID_TOKEN, userDetails.getEmail(), "Access denied for the user");
            throw new AccessDeniedException("You can only access your own profile.");
        }
        UserDTO userDTO = usersService.findUserByID(id);
        return new ResponseEntity<>(userDTO, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUserProfile(@PathVariable Long id, @RequestBody @Valid UserProfileDTO updatedUser, Authentication authentication) {
        log.info("Update userDTO profile for the id {}", id);
        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        if (!userDetails.getId().equals(id)) {
            auditService.auditLog(AuditEventType.INVALID_TOKEN, userDetails.getEmail(), "Access denied for the user");
            throw new AccessDeniedException("You can only access your own profile.");
        }
        UserDTO userDTO = usersService.updateUser(id, updatedUser);
        auditService.auditLog(AuditEventType.PROFILE_UPDATED, userDetails.getEmail(), "User profile updated");
        return new ResponseEntity<>(userDTO, HttpStatus.OK);
    }

    @GetMapping("/accessAll")
    public String accessAll(){
        return "This endpoint can be accessed by all the users!";
    }

    @GetMapping("/adminAccess")
    public String adminAccess(){

        return "This endpoint can be accessed only by the admins!";
    }
}

