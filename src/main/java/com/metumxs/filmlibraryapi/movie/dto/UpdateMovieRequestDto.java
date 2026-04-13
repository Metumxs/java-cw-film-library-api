package com.metumxs.filmlibraryapi.movie.dto;

import jakarta.validation.constraints.*;

import java.util.Set;

public record UpdateMovieRequestDto(
        @NotBlank(message = "title must not be blank")
        @Size(max = 255, message = "title must be less than or equal to 255 characters")
        String title,

        @NotBlank(message = "description must not be blank")
        @Size(max = 2000, message = "description must be less than or equal to 2000 characters")
        String description,

        @NotNull(message = "releaseYear must not be null")
        @Min(value = 1888, message = "releaseYear must be greater than or equal to 1888")
        Integer releaseYear,

        @NotNull(message = "durationMinutes must not be null")
        @Positive(message = "durationMinutes must be greater than 0")
        Integer durationMinutes,

        @NotBlank(message = "country must not be blank")
        @Size(max = 100, message = "country must be less than or equal to 100 characters")
        String country,

        @NotEmpty(message = "genreIds must not be empty")
        Set<@NotNull(message = "genreId must not be null")
        @Positive(message = "genreId must be greater than 0") Long> genreIds
)
{
}