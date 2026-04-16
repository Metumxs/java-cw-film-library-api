package com.metumxs.filmlibraryapi.auth.dto;

public record LoginResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn
)
{
}