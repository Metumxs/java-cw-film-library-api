package com.metumxs.filmlibraryapi.auth.service;

import com.metumxs.filmlibraryapi.auth.dto.LoginRequestDto;
import com.metumxs.filmlibraryapi.auth.dto.LoginResponseDto;
import com.metumxs.filmlibraryapi.auth.dto.RegistrationRequestDto;
import com.metumxs.filmlibraryapi.auth.dto.RegistrationResponseDto;
import com.metumxs.filmlibraryapi.domain.entity.Role;
import com.metumxs.filmlibraryapi.domain.entity.User;
import com.metumxs.filmlibraryapi.domain.repository.RoleRepository;
import com.metumxs.filmlibraryapi.domain.repository.UserRepository;
import com.metumxs.filmlibraryapi.exception.ConflictException;
import com.metumxs.filmlibraryapi.security.JwtTokenService;
import com.metumxs.filmlibraryapi.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest
{
    private static final String TEST_NAME = "Serhii";
    private static final String TEST_EMAIL = "serhii@example.com";
    private static final String TEST_PASSWORD = "strongPass123";
    private static final String HASHED_PASSWORD = "hashed-password";
    private static final String ROLE_USER = "USER";
    private static final Long TEST_USER_ID = 10L;

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

    private Role defaultRole;

    @BeforeEach
    void setUp()
    {
        authService = new AuthService(
                userRepository,
                roleRepository,
                passwordEncoder,
                authenticationManager,
                jwtTokenService
        );

        defaultRole = new Role();
        defaultRole.setId(1L);
        defaultRole.setName(ROLE_USER);
    }

    @Test
    void register_shouldRegisterUserSuccessfully()
    {
        RegistrationRequestDto requestDto = new RegistrationRequestDto(
                TEST_NAME,
                TEST_EMAIL,
                TEST_PASSWORD
        );

        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 1, 1);

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(roleRepository.findByName(ROLE_USER)).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);

        when(userRepository.save(any(User.class))).thenAnswer(invocation ->
        {
            User userToSave = invocation.getArgument(0);
            User savedUser = new User();
            savedUser.setId(TEST_USER_ID);
            savedUser.setName(userToSave.getName());
            savedUser.setEmail(userToSave.getEmail());
            savedUser.setPasswordHash(userToSave.getPasswordHash());
            savedUser.setRole(userToSave.getRole());
            savedUser.setCreatedAt(createdAt);
            return savedUser;
        });

        RegistrationResponseDto result = authService.register(requestDto);

        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.id());
        assertEquals(TEST_NAME, result.name());
        assertEquals(TEST_EMAIL, result.email());
        assertEquals(ROLE_USER, result.role());
        assertEquals(createdAt, result.createdAt());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(TEST_NAME, capturedUser.getName());
        assertEquals(TEST_EMAIL, capturedUser.getEmail());
        assertEquals(HASHED_PASSWORD, capturedUser.getPasswordHash());
        assertEquals(defaultRole, capturedUser.getRole());

        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(roleRepository).findByName(ROLE_USER);
        verify(passwordEncoder).encode(TEST_PASSWORD);
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void register_shouldThrowConflictException_whenEmailAlreadyExists()
    {
        RegistrationRequestDto requestDto = new RegistrationRequestDto(
                TEST_NAME,
                TEST_EMAIL,
                TEST_PASSWORD
        );

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> authService.register(requestDto)
        );

        assertEquals(
                "User with email " + TEST_EMAIL + " already exists",
                exception.getMessage()
        );

        verify(userRepository).existsByEmail(TEST_EMAIL);
        verifyNoInteractions(roleRepository);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void register_shouldThrowIllegalStateException_whenDefaultUserRoleIsMissing()
    {
        RegistrationRequestDto requestDto = new RegistrationRequestDto(
                TEST_NAME,
                TEST_EMAIL,
                TEST_PASSWORD
        );

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(roleRepository.findByName(ROLE_USER)).thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authService.register(requestDto)
        );

        assertEquals("Default role " + ROLE_USER + " not found", exception.getMessage());

        verify(userRepository).existsByEmail(TEST_EMAIL);
        verify(roleRepository).findByName(ROLE_USER);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void register_shouldNormalizeNameAndEmailBeforeCheckingAndSaving()
    {
        String rawName = "  Serhii Khomenko  ";
        String rawEmail = "  SERHII@EXAMPLE.COM  ";
        String expectedNormalizedName = "Serhii Khomenko";

        RegistrationRequestDto requestDto = new RegistrationRequestDto(
                rawName,
                rawEmail,
                TEST_PASSWORD
        );

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(roleRepository.findByName(ROLE_USER)).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);

        when(userRepository.save(any(User.class))).thenAnswer(invocation ->
        {
            User userToSave = invocation.getArgument(0);
            userToSave.setId(1L);
            userToSave.setCreatedAt(LocalDateTime.of(2026, 1, 1, 1, 1));
            return userToSave;
        });

        RegistrationResponseDto result = authService.register(requestDto);

        assertEquals(expectedNormalizedName, result.name());
        assertEquals(TEST_EMAIL, result.email());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(expectedNormalizedName, capturedUser.getName());
        assertEquals(TEST_EMAIL, capturedUser.getEmail());

        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void register_shouldUsePasswordEncoderAndStoreHashedPassword()
    {
        String plainPassword = "plainPassword123";
        String generatedHash = "secure-hash";

        RegistrationRequestDto requestDto = new RegistrationRequestDto(
                TEST_NAME,
                TEST_EMAIL,
                plainPassword
        );

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(roleRepository.findByName(ROLE_USER)).thenReturn(Optional.of(defaultRole));
        when(passwordEncoder.encode(plainPassword)).thenReturn(generatedHash);

        when(userRepository.save(any(User.class))).thenAnswer(invocation ->
        {
            User userToSave = invocation.getArgument(0);
            userToSave.setId(2L);
            userToSave.setCreatedAt(LocalDateTime.of(2026, 1, 1, 1, 1));
            return userToSave;
        });

        authService.register(requestDto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals(generatedHash, capturedUser.getPasswordHash());
        assertNotEquals(plainPassword, capturedUser.getPasswordHash());

        verify(passwordEncoder).encode(plainPassword);
        verifyNoInteractions(authenticationManager);
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void login_shouldReturnAccessTokenSuccessfully()
    {
        LoginRequestDto requestDto = new LoginRequestDto(
                TEST_EMAIL,
                TEST_PASSWORD
        );

        CustomUserDetails userDetails = new CustomUserDetails(
                1L,
                TEST_EMAIL,
                HASHED_PASSWORD,
                ROLE_USER
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenService.generateAccessToken(userDetails))
                .thenReturn("jwt-token-value");
        when(jwtTokenService.getAccessTokenExpiresInSeconds())
                .thenReturn(3600L);

        LoginResponseDto result = authService.login(requestDto);

        assertNotNull(result);
        assertEquals("jwt-token-value", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(3600L, result.expiresInSeconds());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenService).generateAccessToken(userDetails);
        verify(jwtTokenService).getAccessTokenExpiresInSeconds();
        verifyNoInteractions(userRepository);
        verifyNoInteractions(roleRepository);
    }

    @Test
    void login_shouldNormalizeEmailBeforeAuthentication()
    {
        String rawEmail = "  SERHII@EXAMPLE.COM  ";

        LoginRequestDto requestDto = new LoginRequestDto(
                rawEmail,
                TEST_PASSWORD
        );

        CustomUserDetails userDetails = new CustomUserDetails(
                1L,
                TEST_EMAIL,
                HASHED_PASSWORD,
                ROLE_USER
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenService.generateAccessToken(userDetails))
                .thenReturn("jwt-token-value");
        when(jwtTokenService.getAccessTokenExpiresInSeconds())
                .thenReturn(3600L);

        authService.login(requestDto);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);

        verify(authenticationManager).authenticate(authenticationCaptor.capture());

        UsernamePasswordAuthenticationToken capturedToken = authenticationCaptor.getValue();
        assertEquals(TEST_EMAIL, capturedToken.getPrincipal());
        assertEquals(TEST_PASSWORD, capturedToken.getCredentials());
    }

    @Test
    void login_shouldThrowAuthenticationException_whenCredentialsAreInvalid()
    {
        LoginRequestDto requestDto = new LoginRequestDto(
                TEST_EMAIL,
                "wrongPassword"
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(requestDto)
        );

        assertEquals("Bad credentials", exception.getMessage());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenService, never()).generateAccessToken(any());
        verify(jwtTokenService, never()).getAccessTokenExpiresInSeconds();
        verifyNoInteractions(userRepository);
        verifyNoInteractions(roleRepository);
    }

    @Test
    void login_shouldGenerateTokenUsingAuthenticatedPrincipal()
    {
        LoginRequestDto requestDto = new LoginRequestDto(
                TEST_EMAIL,
                TEST_PASSWORD
        );

        CustomUserDetails userDetails = new CustomUserDetails(
                99L,
                TEST_EMAIL,
                HASHED_PASSWORD,
                "ADMIN"
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenService.generateAccessToken(userDetails))
                .thenReturn("admin-jwt-token");
        when(jwtTokenService.getAccessTokenExpiresInSeconds())
                .thenReturn(3600L);

        LoginResponseDto result = authService.login(requestDto);

        assertEquals("admin-jwt-token", result.accessToken());

        verify(jwtTokenService).generateAccessToken(userDetails);
        verify(jwtTokenService).getAccessTokenExpiresInSeconds();
    }
}