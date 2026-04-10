package com.metumxs.filmlibraryapi.movie.dto;

import java.util.Set;

public record MovieSummaryResponseDto(
        Long id,
        String title,
        Integer releaseYear,
        Set<String> genres,
        Double averageRating,
        Long ratingsCount
)
{
}