package com.metumxs.filmlibraryapi.rating;

import com.metumxs.filmlibraryapi.domain.entity.Movie;
import com.metumxs.filmlibraryapi.domain.entity.Rating;
import com.metumxs.filmlibraryapi.domain.entity.User;
import com.metumxs.filmlibraryapi.domain.projection.MovieSummaryProjection;
import com.metumxs.filmlibraryapi.domain.repository.MovieRepository;
import com.metumxs.filmlibraryapi.domain.repository.RatingRepository;
import com.metumxs.filmlibraryapi.domain.repository.UserRepository;
import com.metumxs.filmlibraryapi.exception.ConflictException;
import com.metumxs.filmlibraryapi.exception.NotFoundException;
import com.metumxs.filmlibraryapi.rating.dto.RatingRequestDto;
import com.metumxs.filmlibraryapi.rating.dto.RatingResponseDto;
import com.metumxs.filmlibraryapi.rating.dto.UserRatingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingService
{
    private final RatingRepository ratingRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Transactional
    public RatingResponseDto createRating(Long movieId, Long currentUserId, RatingRequestDto requestDto)
    {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie with id " + movieId + " not found"));

        if (ratingRepository.existsByUser_IdAndMovie_Id(currentUserId, movieId))
        {
            throw new ConflictException("Rating for movie " + movieId + " already exists for current user");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("User with id " + currentUserId + " not found"));

        Rating rating = new Rating();
        rating.setValue(requestDto.value());
        rating.setMovie(movie);
        rating.setUser(user);

        Rating savedRating = ratingRepository.save(rating);

        return RatingResponseDto.fromEntity(savedRating);
    }

    @Transactional
    public RatingResponseDto updateRating(Long movieId, Long currentUserId, RatingRequestDto requestDto)
    {
        if (!movieRepository.existsById(movieId))
        {
            throw new NotFoundException("Movie with id " + movieId + " not found");
        }

        Rating rating = ratingRepository.findByUser_IdAndMovie_Id(currentUserId, movieId)
                .orElseThrow(() -> new NotFoundException(
                        "Rating for movie " + movieId + " by current user not found"
                ));

        rating.updateValue(requestDto.value());

        Rating updatedRating = ratingRepository.save(rating);

        return RatingResponseDto.fromEntity(updatedRating);
    }

    @Transactional
    public void deleteRating(Long movieId, Long currentUserId)
    {
        if (!movieRepository.existsById(movieId))
        {
            throw new NotFoundException("Movie with id " + movieId + " not found");
        }

        Rating rating = ratingRepository.findByUser_IdAndMovie_Id(currentUserId, movieId)
                .orElseThrow(() -> new NotFoundException(
                        "Rating for movie " + movieId + " by current user not found"
                ));

        ratingRepository.delete(rating);
    }

    public List<UserRatingResponseDto> getUserRatings(Long currentUserId)
    {
        List<Rating> ratings = ratingRepository.findAllByUser_Id(currentUserId);

        if (ratings.isEmpty()) {
            return List.of();
        }

        List<Long> movieIds = ratings.stream()
                .map(r -> r.getMovie().getId())
                .distinct()
                .toList();

        List<MovieSummaryProjection> movieSummaries = movieRepository.findAllMovieSummariesByIds(movieIds);

        Map<Long, String> movieTitleMap = movieSummaries.stream()
                .collect(Collectors.toMap(
                        MovieSummaryProjection::getId,
                        MovieSummaryProjection::getTitle
                ));

        return ratings.stream()
                .map(rating -> {
                    Long movieId = rating.getMovie().getId();
                    String title = movieTitleMap.getOrDefault(movieId, "Unknown Title");

                    return new UserRatingResponseDto(
                            movieId,
                            title,
                            rating.getValue()
                    );
                })
                .toList();
    }
}