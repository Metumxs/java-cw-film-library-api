package com.metumxs.filmlibraryapi.rating.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import static com.metumxs.filmlibraryapi.validation.ValidationConstants.RATING_MAX_VALUE;
import static com.metumxs.filmlibraryapi.validation.ValidationConstants.RATING_MIN_VALUE;

public record RatingRequestDto(
        @NotNull(message = "rating value {validation.notNull}")
        @Min(value = RATING_MIN_VALUE, message = "{rating.value.minValue}")
        @Max(value = RATING_MAX_VALUE, message = "{rating.value.maxValue}")
        Integer value
)
{
}