package com.metumxs.filmlibraryapi.domain.repository;

import com.metumxs.filmlibraryapi.domain.entity.Rating;
import com.metumxs.filmlibraryapi.domain.projection.MovieRatingSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long>
{
    boolean existsByUser_IdAndMovie_Id(Long userId, Long movieId);

    Optional<Rating> findByUser_IdAndMovie_Id(Long userId, Long movieId);

    List<Rating> findAllByUser_Id(Long userId);

    @Query("""
            select r.movie.id as movieId,
                   avg(r.value) as averageRating,
                   count(r.id) as ratingsCount
            from Rating r
            where r.movie.id in :movieIds
            group by r.movie.id
            """)
    List<MovieRatingSummaryProjection> findRatingSummariesByMovieIds(@Param("movieIds") Collection<Long> movieIds);

    @Query("""
            select r.movie.id as movieId,
                   avg(r.value) as averageRating,
                   count(r.id) as ratingsCount
            from Rating r
            where r.movie.id = :movieId
            group by r.movie.id
            """)
    Optional<MovieRatingSummaryProjection> findRatingSummaryByMovieId(@Param("movieId") Long movieId);
}