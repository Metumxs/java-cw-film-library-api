package com.metumxs.filmlibraryapi.auth.dto;

import java.time.LocalDateTime;

public record RegistrationResponseDto(
        Long id,
        String name,
        String email,
        String role,
        LocalDateTime createdAt
)
{
}