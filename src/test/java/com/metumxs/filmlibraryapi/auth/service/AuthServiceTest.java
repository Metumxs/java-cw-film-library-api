package com.metumxs.filmlibraryapi.auth.service;

import com.metumxs.filmlibraryapi.auth.dto.RegistrationRequestDto;
import com.metumxs.filmlibraryapi.auth.dto.RegistrationResponseDto;
import com.metumxs.filmlibraryapi.domain.entity.Role;
import com.metumxs.filmlibraryapi.domain.entity.User;
import com.metumxs.filmlibraryapi.domain.repository.RoleRepository;
import com.metumxs.filmlibraryapi.domain.repository.UserRepository;
import com.metumxs.filmlibraryapi.exception.ConflictException;
import com.metumxs.filmlibraryapi.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest
{
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp()
    {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, authenticationManager, jwtTokenService);
    }

    @Test
    void register_shouldRegisterUserSuccessfully()
    {
        RegistrationRequestDto requestDto = new RegistrationRequestDto(
                "  Serhii  ",
                "  Serhii@Example.COM  ",
                "strongPass123"
        );

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 14, 20, 0);

        when(userRepository.existsByEmail("serhii@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("strongPass123")).thenReturn("hashed-password");

        when(userRepository.save(any(User.class))).thenAnswer(invocation ->
        {
            User userToSave = invocation.getArgument(0);

            User savedUser = new User();
            savedUser.setId(10L);
            savedUser.setName(userToSave.getName());
            savedUser.setEmail(userToSave.getEmail());
            savedUser.setPasswordHash(userToSave.getPasswordHash());
            savedUser.setRole(userToSave.getRole());
            savedUser.setCreatedAt(createdAt);

            return savedUser;
        });

        RegistrationResponseDto result = authService.register(requestDto);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("Serhii", result.name());
        assertEquals("serhii@example.com", result.email());
        assertEquals("USER", result.role());
        assertEquals(createdAt, result.createdAt());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals("Serhii", capturedUser.getName());
        assertEquals("serhii@example.com", capturedUser.getEmail());
        assertEquals("hashed-password", capturedUser.getPasswordHash());
        assertEquals(userRole, capturedUser.getRole());

        verify(userRepository).existsByEmail("serhii@example.com");
        verify(roleRepository).findByName("USER");
        verify(passwordEncoder).encode("strongPass123");
    }

    @Test
    void register_shouldThrowConflictException_whenEmailAlreadyExists()
    {
        RegistrationRequestDto requestDto = new RegistrationRequestDto(
                "Serhii",
                "serhii@example.com",
                "strongPass123"
        );

        when(userRepository.existsByEmail("serhii@example.com")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> authService.register(requestDto)
        );

        assertEquals(
                "User with email serhii@example.com already exists",
                exception.getMessage()
        );

        verify(userRepository).existsByEmail("serhii@example.com");
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldThrowIllegalStateException_whenDefaultUserRoleIsMissing()
    {
        RegistrationRequestDto requestDto = new RegistrationRequestDto(
                "Serhii",
                "serhii@example.com",
                "strongPass123"
        );

        when(userRepository.existsByEmail("serhii@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authService.register(requestDto)
        );

        assertEquals("Default role USER not found", exception.getMessage());

        verify(userRepository).existsByEmail("serhii@example.com");
        verify(roleRepository).findByName("USER");
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldNormalizeNameAndEmailBeforeCheckingAndSaving()
    {
        RegistrationRequestDto requestDto = new RegistrationRequestDto(
                "  Serhii Khomenko  ",
                "  SERHII@EXAMPLE.COM  ",
                "strongPass123"
        );

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        when(userRepository.existsByEmail("serhii@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("strongPass123")).thenReturn("hashed-password");

        when(userRepository.save(any(User.class))).thenAnswer(invocation ->
        {
            User userToSave = invocation.getArgument(0);
            userToSave.setId(1L);
            userToSave.setCreatedAt(LocalDateTime.of(2026, 4, 14, 21, 0));
            return userToSave;
        });

        RegistrationResponseDto result = authService.register(requestDto);

        assertEquals("Serhii Khomenko", result.name());
        assertEquals("serhii@example.com", result.email());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals("Serhii Khomenko", capturedUser.getName());
        assertEquals("serhii@example.com", capturedUser.getEmail());
    }

    @Test
    void register_shouldUsePasswordEncoderAndStoreHashedPassword()
    {
        RegistrationRequestDto requestDto = new RegistrationRequestDto(
                "Serhii",
                "serhii@example.com",
                "plainPassword123"
        );

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        when(userRepository.existsByEmail("serhii@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("plainPassword123")).thenReturn("secure-hash");

        when(userRepository.save(any(User.class))).thenAnswer(invocation ->
        {
            User userToSave = invocation.getArgument(0);
            userToSave.setId(2L);
            userToSave.setCreatedAt(LocalDateTime.of(2026, 4, 14, 22, 0));
            return userToSave;
        });

        authService.register(requestDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals("secure-hash", capturedUser.getPasswordHash());
        assertNotEquals("plainPassword123", capturedUser.getPasswordHash());

        verify(passwordEncoder).encode("plainPassword123");
    }
}