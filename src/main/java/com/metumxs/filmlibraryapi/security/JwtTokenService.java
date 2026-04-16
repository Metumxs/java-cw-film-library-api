package com.metumxs.filmlibraryapi.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtTokenService
{
    private static final long ACCESS_TOKEN_EXPIRES_IN_SECONDS = 3600L;

    private final JwtEncoder jwtEncoder;

    public String generateAccessToken(SecurityUserDetails userDetails)
    {
        Instant now = Instant.now();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer("film-library-api")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_EXPIRES_IN_SECONDS))
                .subject(userDetails.getEmail())
                .claim("userId", userDetails.getUserId())
                .claim("email", userDetails.getEmail())
                .claim("role", userDetails.getRole())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claimsSet))
                .getTokenValue();
    }

    public long getAccessTokenExpiresInSeconds()
    {
        return ACCESS_TOKEN_EXPIRES_IN_SECONDS;
    }
}