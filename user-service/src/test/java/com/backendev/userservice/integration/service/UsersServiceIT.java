package com.backendev.userservice.integration.service;

import com.backendev.userservice.dto.UserDTO;
import com.backendev.userservice.dto.UserProfileDTO;
import com.backendev.userservice.dto.UserRegistrationRequest;
import com.backendev.userservice.dto.UserRegistrationResponse;
import com.backendev.userservice.entity.Users;
import com.backendev.userservice.exception.UserAlreadyExistsException;
import com.backendev.userservice.messaging.UserEventPublisher;
import com.backendev.userservice.repository.UsersRepository;
import com.backendev.userservice.service.UsersService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UsersServiceIT {

    @Autowired
    private UsersService usersService;

    @Autowired
    private UsersRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserEventPublisher userEventPublisher;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        doNothing().when(userEventPublisher).publishUserEvent(any());
    }

    @Test
    void testRegisterUser_Success() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();

        UserRegistrationResponse response = usersService.registerUser(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");

        Users savedUser = userRepository.findByEmail("john@example.com").orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(savedUser.getFirstName()).isEqualTo("John");
    }

    @Test
    void testRegisterUser_shouldPublishKafkaEvent_onSuccess() {
        UserRegistrationRequest request = buildRequest();

        usersService.registerUser(request);

        verify(userEventPublisher, times(1)).publishUserEvent(argThat(event ->
                "REGISTRATION".equals(event.getEventType()) &&
                        "john@example.com".equals(event.getEmail())
        ));
    }

    @Test
    void testRegisterUser_shouldNotPublishKafkaEvent_whenUserAlreadyExists() {
        UserRegistrationRequest request = buildRequest();
        usersService.registerUser(request);

        // reset so we only count the second call
        reset(userEventPublisher);
        doNothing().when(userEventPublisher).publishUserEvent(any());

        assertThatThrownBy(() -> usersService.registerUser(request))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userEventPublisher, never()).publishUserEvent(any());
    }

    @Test
    void testRegisterUser_PasswordIsEncoded() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();

        usersService.registerUser(request);

        // Verify password is hashed, not stored as plain text
        Users savedUser = userRepository.findByEmail("john@example.com").orElse(null);
        Assertions.assertNotNull(savedUser);
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();
    }

    @Test
    void testRegisterUser_RolesAssigned() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();

        usersService.registerUser(request);

        Users savedUser = userRepository.findByEmail("john@example.com").orElse(null);
        Assertions.assertNotNull(savedUser);
        assertThat(savedUser.getRoles()).isNotEmpty();
        assertThat(savedUser.getRoles()).extracting("name").contains("ROLE_USER");
    }

    @Test
    void testRegisterUser_UserAlreadyExists() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();

        usersService.registerUser(request);

        assertThatThrownBy(() -> usersService.registerUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("User already exists");
    }

    @Test
    void testRegisterUser_InvalidRole() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("INVALID_ROLE"))
                .build();

        assertThatThrownBy(() -> usersService.registerUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Role not found");
    }

    @Test
    void testFindUserByID_Success() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();
        UserRegistrationResponse registered = usersService.registerUser(request);

        UserDTO userDTO = usersService.findUserByID(registered.getId());

        assertThat(userDTO).isNotNull();
        assertThat(userDTO.getEmail()).isEqualTo("john@example.com");
        assertThat(userDTO.getFirstName()).isEqualTo("John");

        verify(userEventPublisher, times(1)).publishUserEvent(any()); // only from register

    }

    @Test
    void testFindUserByID_UserNotFound() {
        assertThatThrownBy(() -> usersService.findUserByID(999L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testUpdateUser_Success() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();
        UserRegistrationResponse registered = usersService.registerUser(request);

        UserProfileDTO updateRequest = new UserProfileDTO("John", "Doe Updated", "9876543210");
        UserDTO updated = usersService.updateUser(registered.getId(), updateRequest);

        assertThat(updated.getLastName()).isEqualTo("Doe Updated");
        assertThat(updated.getPhone()).isEqualTo("9876543210");

        Users savedUser = userRepository.findById(registered.getId()).orElse(null);
        Assertions.assertNotNull(savedUser);
        assertThat(savedUser.getLastName()).isEqualTo("Doe Updated");
    }

    @Test
    void testUpdateUser_shouldPublishKafkaEvent_onSuccess() {
        UserRegistrationRequest request = buildRequest();
        UserRegistrationResponse registered = usersService.registerUser(request);

        // reset so we only count the update call
        reset(userEventPublisher);
        doNothing().when(userEventPublisher).publishUserEvent(any());

        UserProfileDTO updateRequest = new UserProfileDTO("John", "Doe Updated", "9876543210");
        usersService.updateUser(registered.getId(), updateRequest);

        verify(userEventPublisher, times(1)).publishUserEvent(argThat(event ->
                "PROFILE_UPDATED".equals(event.getEventType()) &&
                        "john@example.com".equals(event.getEmail())
        ));
    }

    @Test
    void testUpdateUser_shouldNotPublishKafkaEvent_whenUserNotFound() {
        UserProfileDTO updateRequest = new UserProfileDTO("Jane", "Doe", "9876543210");

        assertThatThrownBy(() -> usersService.updateUser(999L, updateRequest))
                .isInstanceOf(RuntimeException.class);

        verify(userEventPublisher, never()).publishUserEvent(any());
    }

    @Test
    void testUpdateUser_InvalidUser() {
        UserProfileDTO updateRequest = new UserProfileDTO("Jane", "Doe", "9876543210");

        assertThatThrownBy(() -> usersService.updateUser(999L, updateRequest))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void testRegisterUser_AllFieldsMapped() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();

        UserRegistrationResponse response = usersService.registerUser(request);

        assertThat(response.getFirstName()).isEqualTo(request.getFirstName());
        assertThat(response.getLastName()).isEqualTo(request.getLastName());
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getPhone()).isEqualTo(request.getPhone());
    }

    private UserRegistrationRequest buildRequest() {
        return UserRegistrationRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .phone("1234567890")
                .roles(Set.of("ROLE_USER"))
                .build();
    }
}
