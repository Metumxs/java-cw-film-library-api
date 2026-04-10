package com.metumxs.filmlibraryapi.domain.projection;

public interface MovieRatingSummaryProjection
{
    Long getMovieId();

    Double getAverageRating();

    Long getRatingsCount();
}