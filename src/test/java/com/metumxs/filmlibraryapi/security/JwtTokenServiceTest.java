package com.metumxs.filmlibraryapi.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest
{
    private static final Long TEST_USER_ID = 99L;
    private static final String TEST_EMAIL = "admin@example.com";
    private static final String TEST_PASSWORD = "hashed-password";
    private static final String TEST_ROLE = "ADMIN";
    private static final String EXPECTED_ISSUER = "film-library-api";
    private static final String MOCK_TOKEN_VALUE = "jwt-token-value";
    private static final long EXPIRATION_SECONDS = 3600L;

    @Mock
    private JwtEncoder jwtEncoder;

    private JwtTokenService jwtTokenService;

    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp()
    {
        jwtTokenService = new JwtTokenService(jwtEncoder, 3600L);

        testUserDetails = new CustomUserDetails(
                TEST_USER_ID,
                TEST_EMAIL,
                TEST_PASSWORD,
                TEST_ROLE
        );
    }

    @Test
    void generateAccessToken_shouldBuildJwtWithExpectedClaims()
    {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(EXPIRATION_SECONDS);

        Jwt encodedJwt = new Jwt(
                MOCK_TOKEN_VALUE,
                issuedAt,
                expiresAt,
                Map.of("alg", "RS256"),
                Map.of("sub", TEST_EMAIL)
        );

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encodedJwt);

        String token = jwtTokenService.generateAccessToken(testUserDetails);

        assertEquals(MOCK_TOKEN_VALUE, token);

        ArgumentCaptor<JwtEncoderParameters> parametersCaptor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(parametersCaptor.capture());

        JwtClaimsSet claimsSet = parametersCaptor.getValue().getClaims();

        assertNotNull(claimsSet);
        assertEquals(EXPECTED_ISSUER, claimsSet.getClaimAsString("iss"));
        assertEquals(TEST_EMAIL, claimsSet.getSubject());
        assertEquals(TEST_USER_ID, claimsSet.getClaim("userId"));
        assertEquals(TEST_EMAIL, claimsSet.getClaim("email"));
        assertEquals(TEST_ROLE, claimsSet.getClaim("role"));

        assertNotNull(claimsSet.getIssuedAt());
        assertNotNull(claimsSet.getExpiresAt());
        assertTrue(claimsSet.getExpiresAt().isAfter(claimsSet.getIssuedAt()));
        assertEquals(
                EXPIRATION_SECONDS,
                Duration.between(claimsSet.getIssuedAt(), claimsSet.getExpiresAt()).getSeconds()
        );
    }

    @Test
    void getAccessTokenExpiresInSeconds_shouldReturnExpectedExpiration()
    {
        long expiresIn = jwtTokenService.getAccessTokenExpiresInSeconds();

        assertEquals(EXPIRATION_SECONDS, expiresIn);
        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void generateAccessToken_shouldThrowIllegalStateException_whenJwtEncoderFails()
    {
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenThrow(new IllegalStateException("JWT encoding failed"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> jwtTokenService.generateAccessToken(testUserDetails)
        );

        assertEquals("JWT encoding failed", exception.getMessage());

        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }
}