package com.metumxs.filmlibraryapi.rating.dto;

public record UserRatingResponseDto(
        Long movieId,
        String movieTitle,
        Integer value
)
{
}