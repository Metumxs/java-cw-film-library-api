package com.metumxs.filmlibraryapi.rating.dto;

import java.time.LocalDateTime;

public record RatingResponseDto(
        Long id,
        Long movieId,
        Long userId,
        Integer value,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
)
{
}