package com.metumxs.filmlibraryapi.movie.dto;

import jakarta.validation.constraints.*;
import java.util.Set;
import static com.metumxs.filmlibraryapi.validation.ValidationConstants.*;

public record CreateMovieRequestDto(
        @NotBlank(message = "title {validation.notBlank}")
        @Size(max = MOVIE_TITLE_MAX_LENGTH, message = "{movie.title.maxSize}")
        String title,

        @NotBlank(message = "description {validation.notBlank}")
        @Size(max = MOVIE_DESC_MAX_LENGTH, message = "{movie.description.maxSize}")
        String description,

        @NotNull(message = "releaseYear {validation.notNull}")
        @Min(value = MOVIE_MIN_YEAR, message = "{movie.releaseYear.minValue}")
        Integer releaseYear,

        @NotNull(message = "durationMinutes {validation.notNull}")
        @Positive(message = "{movie.duration.positive}")
        Integer durationMinutes,

        @NotBlank(message = "country {validation.notBlank}")
        @Size(max = MOVIE_COUNTRY_MAX_LENGTH, message = "{movie.country.maxSize}")
        String country,

        @NotEmpty(message = "genreIds {validation.notEmpty}")
        Set<@NotNull(message = "genreId {validation.notNull}")
        @Positive(message = "{movie.genre.positive}") Long> genreIds
)
{
}