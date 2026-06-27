package com.backendev.userservice.integration.controller;

import com.backendev.userservice.dto.AuthRequest;
import com.backendev.userservice.dto.UserProfileDTO;
import com.backendev.userservice.dto.UserRegistrationRequest;
import com.backendev.userservice.dto.UserRegistrationResponse;
import com.backendev.userservice.entity.Roles;
import com.backendev.userservice.entity.Users;
import com.backendev.userservice.repository.UsersRepository;
import com.backendev.userservice.security.AppUserDetails;
import com.backendev.userservice.service.LoginService;
import com.backendev.userservice.service.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class UsersControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsersService usersService;

    @Mock
    private LoginService loginService;

    @Autowired
    private UsersRepository usersRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "/api/v1/users";

    @BeforeEach
    void setUp() {
        usersRepository.deleteAll();
    }

    @Test
    void testRegisterUser_Success() throws Exception {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void testRegisterUser_InvalidEmail() throws Exception {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("invalid-email")
                .password("password123")
                .phone("1234567890")
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogin_Success() throws Exception {
        UserRegistrationRequest regRequest = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();
        UserRegistrationResponse registered = usersService.registerUser(regRequest);

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

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        AuthRequest request = new AuthRequest("john@example.com", "wrongpassword");

        when(loginService.login(request)).thenThrow(new BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetUserProfile_Success() throws Exception {
        UserRegistrationRequest regRequest = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();
        UserRegistrationResponse registered = usersService.registerUser(regRequest);
        Long userId = registered.getId();

        mockMvc.perform(get(BASE_URL + "/" + userId)
                        .with(authentication(createMockAuthentication(userId, "john@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void testGetUserProfile_AccessDenied() throws Exception {
        UserRegistrationRequest regRequest = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();
        UserRegistrationResponse registered = usersService.registerUser(regRequest);
        Long userId = registered.getId();

        mockMvc.perform(get(BASE_URL + "/" + (userId + 1))
                        .with(authentication(createMockAuthentication(userId, "john@example.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateUserProfile_Success() throws Exception {
        UserRegistrationRequest regRequest = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();
        UserRegistrationResponse registered = usersService.registerUser(regRequest);
        Long userId = registered.getId();

        UserProfileDTO updateRequest = new UserProfileDTO("John", "Doe Updated", "9876543210");

        mockMvc.perform(put(BASE_URL + "/" + userId)
                        .with(authentication(createMockAuthentication(userId, "john@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Doe Updated"));
    }

    @Test
    void testUpdateUserProfile_AccessDenied() throws Exception {
        // Register user with ID 1
        UserRegistrationRequest regRequest = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();
        UserRegistrationResponse registered = usersService.registerUser(regRequest);
        Long userId = registered.getId();

        UserProfileDTO updateRequest = new UserProfileDTO("Jane", "Doe", "9876543210");

        mockMvc.perform(put(BASE_URL + "/" + (userId + 1))
                        .with(authentication(createMockAuthentication(userId, "john@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAccessAll() throws Exception {
        mockMvc.perform(get(BASE_URL + "/accessAll"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("This endpoint can be accessed by all the users!")));
    }

    private Authentication createMockAuthentication(Long userId, String email) {
        Roles userRole = new Roles();
        userRole.setName("ROLE_USER");

        Users mockUser = new Users();
        mockUser.setId(userId);
        mockUser.setEmail(email);
        mockUser.setPassword("password");
        mockUser.setRoles(Set.of(userRole));

        AppUserDetails userDetails = new AppUserDetails(mockUser);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}
