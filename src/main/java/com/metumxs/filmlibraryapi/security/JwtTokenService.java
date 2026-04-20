package com.metumxs.filmlibraryapi.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class JwtTokenService
{
    @Getter
    @Value("${jwt.expiration-seconds:3600}")
    private long accessTokenExpiresInSeconds;

    private final JwtEncoder jwtEncoder;

    public JwtTokenService(JwtEncoder jwtEncoder,
                           @Value("${jwt.expiration-seconds:3600}") long accessTokenExpiresInSeconds)
    {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenExpiresInSeconds = accessTokenExpiresInSeconds;
    }

    public String generateAccessToken(CustomUserDetails userDetails)
    {
        Instant now = Instant.now();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer("film-library-api")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenExpiresInSeconds))
                .subject(userDetails.email())
                .claim("userId", userDetails.userId())
                .claim("email", userDetails.email())
                .claim("role", userDetails.role())
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claimsSet))
                .getTokenValue();
    }

}