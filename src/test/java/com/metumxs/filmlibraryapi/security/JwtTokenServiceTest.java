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
    @Mock
    private JwtEncoder jwtEncoder;

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp()
    {
        jwtTokenService = new JwtTokenService(jwtEncoder);
    }

    @Test
    void generateAccessToken_shouldBuildJwtWithExpectedClaims()
    {
        CustomUserDetails userDetails = new CustomUserDetails(
                99L,
                "admin@example.com",
                "hashed-password",
                "ADMIN"
        );

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(3600);

        Jwt encodedJwt = new Jwt(
                "jwt-token-value",
                issuedAt,
                expiresAt,
                Map.of("alg", "RS256"),
                Map.of("sub", "admin@example.com")
        );

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encodedJwt);

        String token = jwtTokenService.generateAccessToken(userDetails);

        assertEquals("jwt-token-value", token);

        ArgumentCaptor<JwtEncoderParameters> parametersCaptor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(parametersCaptor.capture());

        JwtClaimsSet claimsSet = parametersCaptor.getValue().getClaims();

        assertNotNull(claimsSet);
        assertEquals("film-library-api", claimsSet.getClaimAsString("iss"));
        assertEquals("admin@example.com", claimsSet.getSubject());
        assertEquals(99L, (Long) claimsSet.getClaim("userId"));
        assertEquals("admin@example.com", claimsSet.getClaim("email"));
        assertEquals("ADMIN", claimsSet.getClaim("role"));

        assertNotNull(claimsSet.getIssuedAt());
        assertNotNull(claimsSet.getExpiresAt());
        assertTrue(claimsSet.getExpiresAt().isAfter(claimsSet.getIssuedAt()));
        assertEquals(
                3600L,
                Duration.between(claimsSet.getIssuedAt(), claimsSet.getExpiresAt()).getSeconds()
        );
    }

    @Test
    void getAccessTokenExpiresInSeconds_shouldReturn3600()
    {
        long expiresIn = jwtTokenService.getAccessTokenExpiresInSeconds();

        assertEquals(3600L, expiresIn);
        verifyNoInteractions(jwtEncoder);
    }

    @Test
    void generateAccessToken_shouldThrowIllegalStateException_whenJwtEncoderFails()
    {
        CustomUserDetails userDetails = new CustomUserDetails(
                1L,
                "user@example.com",
                "hashed-password",
                "USER"
        );

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenThrow(new IllegalStateException("JWT encoding failed"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> jwtTokenService.generateAccessToken(userDetails)
        );

        assertEquals("JWT encoding failed", exception.getMessage());

        verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
    }
}