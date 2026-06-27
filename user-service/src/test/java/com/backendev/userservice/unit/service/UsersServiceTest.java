package com.backendev.userservice.unit.service;

import com.backendev.userservice.dto.NotificationEvent;
import com.backendev.userservice.dto.UserDTO;
import com.backendev.userservice.dto.UserProfileDTO;
import com.backendev.userservice.dto.UserRegistrationRequest;
import com.backendev.userservice.dto.UserRegistrationResponse;
import com.backendev.userservice.entity.Roles;
import com.backendev.userservice.entity.Users;
import com.backendev.userservice.exception.UserAlreadyExistsException;
import com.backendev.userservice.exception.UserNotFoundException;
import com.backendev.userservice.mapper.UserMapper;
import com.backendev.userservice.messaging.UserEventPublisher;
import com.backendev.userservice.repository.RolesRepository;
import com.backendev.userservice.repository.UsersRepository;
import com.backendev.userservice.service.UsersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {
    @Mock
    private UsersRepository usersRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEventPublisher userEventPublisher;

    @InjectMocks
    private UsersService usersService;

    private UserRegistrationRequest registrationRequest;
    private Users userEntity;
    private UserRegistrationResponse registrationResponse;
    private Roles roles;

    @BeforeEach
    void setUp() {
        roles = new Roles(1L, "USER_ROLE");
        registrationRequest = new UserRegistrationRequest("John", "Doe","johndoe@test.com",
                "password123", "436587905", Set.of("ROLE_USER"));

        userEntity = new Users();
        userEntity.setId(1L);
        userEntity.setEmail("johndoe@test.com");
        userEntity.setPassword("password123");
        userEntity.setRoles(new HashSet<>());

        registrationResponse = new UserRegistrationResponse(1L, "John", "Doe","johndoe@test.com",
                 "436587905", Set.of("ROLE_USER"));
    }

    @Test
    void registerUser_shouldSaveUserAndReturnResponse_whenUserDoesNotExist() {
        when(usersRepository.existsByEmail("johndoe@test.com")).thenReturn(false);
        when(userMapper.toEntity(registrationRequest)).thenReturn(userEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usersRepository.save(userEntity)).thenReturn(userEntity);
        when(rolesRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roles));
        when(userMapper.toResponse(userEntity)).thenReturn(registrationResponse);

        UserRegistrationResponse response = usersService.registerUser(registrationRequest);

        assertEquals("johndoe@test.com", response.getEmail());
        assertEquals(Set.of("ROLE_USER"), response.getRoles());
        verify(usersRepository, times(2)).save(userEntity); // once before roles, once after
    }

    @Test
    void registerUser_shouldPublishNotificationEvent_whenRegistrationSucceeds() {
        when(usersRepository.existsByEmail("johndoe@test.com")).thenReturn(false);
        when(userMapper.toEntity(registrationRequest)).thenReturn(userEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usersRepository.save(userEntity)).thenReturn(userEntity);
        when(rolesRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roles));
        when(userMapper.toResponse(userEntity)).thenReturn(registrationResponse);

        usersService.registerUser(registrationRequest);

        // verify Kafka event was published exactly once
        verify(userEventPublisher, times(1)).publishUserEvent(any(NotificationEvent.class));
    }

    @Test
    void registerUser_shouldPublishEventWithCorrectDetails_whenRegistrationSucceeds() {
        when(usersRepository.existsByEmail("johndoe@test.com")).thenReturn(false);
        when(userMapper.toEntity(registrationRequest)).thenReturn(userEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usersRepository.save(userEntity)).thenReturn(userEntity);
        when(rolesRepository.findByName("ROLE_USER")).thenReturn(Optional.of(roles));
        when(userMapper.toResponse(userEntity)).thenReturn(registrationResponse);

        usersService.registerUser(registrationRequest);

        // verify the event contains correct data
        verify(userEventPublisher).publishUserEvent(argThat(event ->
                "REGISTRATION".equals(event.getEventType()) &&
                        "johndoe@test.com".equals(event.getEmail()) &&
                        "Registration Successful".equals(event.getSubject())
        ));
    }

    @Test
    void registerUser_shouldNotPublishEvent_whenEmailAlreadyExists() {
        when(usersRepository.existsByEmail("johndoe@test.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> usersService.registerUser(registrationRequest));

        // event must NOT be published if registration failed
        verify(userEventPublisher, never()).publishUserEvent(any());
    }

    @Test
    void registerUser_shouldThrowException_whenEmailAlreadyExists() {
        when(usersRepository.existsByEmail("johndoe@test.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> usersService.registerUser(registrationRequest));

        verify(usersRepository, never()).save(any());
    }

    @Test
    void registerUser_shouldThrowException_whenRoleNotFound() {
        when(usersRepository.existsByEmail("johndoe@example.com")).thenReturn(false);
        when(userMapper.toEntity(registrationRequest)).thenReturn(userEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(usersRepository.save(userEntity)).thenReturn(userEntity);
        when(rolesRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> usersService.registerUser(registrationRequest), "Expected RuntimeException for missing role");
    }

    @Test
    void findUserByID_shouldReturnUserDTO_whenUserExists() {
        when(usersRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        UserDTO userDTO = new UserDTO("John","Doe", "johndoe@example.com","436587905");
        when(userMapper.toDto(userEntity)).thenReturn(userDTO);

        UserDTO result = usersService.findUserByID(1L);

        assertEquals("johndoe@example.com", result.getEmail());

        verify(userEventPublisher, never()).publishUserEvent(any());

    }

    @Test
    void findUserByID_shouldThrowException_whenUserNotFound() {
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> usersService.findUserByID(1L));
    }

    @Test
    void updateUser_shouldUpdateAndReturnUserDTO_whenUserExists() {
        UserProfileDTO profileDTO = new UserProfileDTO("John","Doe","436587905");
        when(usersRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        doNothing().when(userMapper).updateUserFromDto(profileDTO, userEntity);
        when(usersRepository.save(userEntity)).thenReturn(userEntity);
        UserDTO updatedDTO = new UserDTO("John","Doe", "johndoe@example.com","436587909");
        when(userMapper.toDto(userEntity)).thenReturn(updatedDTO);

        UserDTO result = usersService.updateUser(1L, profileDTO);

        assertEquals("436587909", result.getPhone());
    }

    @Test
    void updateUser_shouldPublishNotificationEvent_whenUpdateSucceeds() {
        UserProfileDTO profileDTO = new UserProfileDTO("John", "Doe", "436587905");
        when(usersRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        doNothing().when(userMapper).updateUserFromDto(profileDTO, userEntity);
        when(usersRepository.save(userEntity)).thenReturn(userEntity);
        UserDTO updatedDTO = new UserDTO("John", "Doe", "johndoe@example.com", "436587909");
        when(userMapper.toDto(userEntity)).thenReturn(updatedDTO);

        usersService.updateUser(1L, profileDTO);

        verify(userEventPublisher, times(1)).publishUserEvent(any(NotificationEvent.class));
    }

    @Test
    void updateUser_shouldPublishEventWithCorrectDetails_whenUpdateSucceeds() {
        UserProfileDTO profileDTO = new UserProfileDTO("John", "Doe", "436587905");
        when(usersRepository.findById(1L)).thenReturn(Optional.of(userEntity));
        doNothing().when(userMapper).updateUserFromDto(profileDTO, userEntity);
        when(usersRepository.save(userEntity)).thenReturn(userEntity);
        UserDTO updatedDTO = new UserDTO("John", "Doe", "johndoe@example.com", "436587909");
        when(userMapper.toDto(userEntity)).thenReturn(updatedDTO);

        usersService.updateUser(1L, profileDTO);

        verify(userEventPublisher).publishUserEvent(argThat(event ->
                "PROFILE_UPDATED".equals(event.getEventType()) &&
                        "johndoe@example.com".equals(event.getEmail()) &&
                        "User Profile changed".equals(event.getSubject())
        ));
    }

    @Test
    void updateUser_shouldNotPublishEvent_whenUserNotFound() {
        UserProfileDTO profileDTO = new UserProfileDTO("John", "Doe", "436587905");
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> usersService.updateUser(1L, profileDTO));

        verify(userEventPublisher, never()).publishUserEvent(any());
    }


    @Test
    void updateUser_shouldThrowException_whenUserNotFound() {
        UserProfileDTO profileDTO = new UserProfileDTO("John","Doe","436587905");
        when(usersRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> usersService.updateUser(1L, profileDTO));
    }
}