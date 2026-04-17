package com.metumxs.filmlibraryapi.rating.dto;

import com.metumxs.filmlibraryapi.domain.entity.Rating;

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
    public static RatingResponseDto fromEntity(Rating rating)
    {
        return new RatingResponseDto(
                rating.getId(),
                rating.getMovie().getId(),
                rating.getUser().getId(),
                rating.getValue(),
                rating.getCreatedAt(),
                rating.getUpdatedAt()
        );
    }
}