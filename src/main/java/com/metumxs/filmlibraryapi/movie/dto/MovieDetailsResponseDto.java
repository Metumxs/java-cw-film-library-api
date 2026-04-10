package com.metumxs.filmlibraryapi.movie.dto;

import java.util.Set;

public record MovieDetailsResponseDto(
        Long id,
        String title,
        String description,
        Integer releaseYear,
        Integer durationMinutes,
        String country,
        Set<String> genres,
        Double averageRating,
        Long ratingsCount
)
{
}