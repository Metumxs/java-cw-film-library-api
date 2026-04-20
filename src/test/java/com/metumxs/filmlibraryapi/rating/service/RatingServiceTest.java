package com.metumxs.filmlibraryapi.rating.service;

import com.metumxs.filmlibraryapi.domain.entity.Movie;
import com.metumxs.filmlibraryapi.domain.entity.Rating;
import com.metumxs.filmlibraryapi.domain.entity.User;
import com.metumxs.filmlibraryapi.domain.repository.MovieRepository;
import com.metumxs.filmlibraryapi.domain.repository.RatingRepository;
import com.metumxs.filmlibraryapi.domain.repository.UserRepository;
import com.metumxs.filmlibraryapi.exception.ConflictException;
import com.metumxs.filmlibraryapi.exception.NotFoundException;
import com.metumxs.filmlibraryapi.rating.dto.RatingRequestDto;
import com.metumxs.filmlibraryapi.rating.dto.RatingResponseDto;
import com.metumxs.filmlibraryapi.rating.dto.UserRatingResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.metumxs.filmlibraryapi.validation.ValidationConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest
{
    private static final int VALID_RATING_VALUE = RATING_MAX_VALUE;
    private static final int UPDATED_RATING_VALUE = RATING_MAX_VALUE - 1;
    private static final int EXISTING_RATING_VALUE = RATING_MAX_VALUE - 2;

    private static final Long TEST_MOVIE_ID = 1L;
    private static final Long TEST_USER_ID = 10L;
    private static final Long TEST_RATING_ID = 100L;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private UserRepository userRepository;

    private RatingService ratingService;

    private Movie testMovie;
    private User testUser;

    @BeforeEach
    void setUp()
    {
        ratingService = new RatingService(ratingRepository, movieRepository, userRepository);
        testMovie = buildMovie(TEST_MOVIE_ID, "Inception");
        testUser = buildUser(TEST_USER_ID, "serhii@example.com");
    }

    @Test
    void createRating_shouldCreateRatingSuccessfully()
    {
        RatingRequestDto requestDto = new RatingRequestDto(VALID_RATING_VALUE);
        Rating savedRating = buildRating(TEST_RATING_ID, VALID_RATING_VALUE, testMovie, testUser);

        when(movieRepository.findById(TEST_MOVIE_ID)).thenReturn(Optional.of(testMovie));
        when(ratingRepository.existsByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(ratingRepository.save(any(Rating.class))).thenReturn(savedRating);

        RatingResponseDto result = ratingService.createRating(TEST_MOVIE_ID, TEST_USER_ID, requestDto);

        assertNotNull(result);
        assertEquals(TEST_RATING_ID, result.id());
        assertEquals(TEST_MOVIE_ID, result.movieId());
        assertEquals(TEST_USER_ID, result.userId());
        assertEquals(VALID_RATING_VALUE, result.value());

        ArgumentCaptor<Rating> ratingCaptor = ArgumentCaptor.forClass(Rating.class);
        verify(ratingRepository).save(ratingCaptor.capture());

        Rating capturedRating = ratingCaptor.getValue();
        assertEquals(VALID_RATING_VALUE, capturedRating.getValue());
        assertEquals(testMovie, capturedRating.getMovie());
        assertEquals(testUser, capturedRating.getUser());
    }

    @Test
    void createRating_shouldThrowNotFoundException_whenMovieDoesNotExist()
    {
        RatingRequestDto requestDto = new RatingRequestDto(VALID_RATING_VALUE);

        when(movieRepository.findById(TEST_MOVIE_ID)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> ratingService.createRating(TEST_MOVIE_ID, TEST_USER_ID, requestDto)
        );

        assertEquals("Movie with id " + TEST_MOVIE_ID + " not found", exception.getMessage());

        verify(movieRepository).findById(TEST_MOVIE_ID);
        verifyNoInteractions(userRepository);
        verifyNoMoreInteractions(ratingRepository);
    }

    @Test
    void createRating_shouldThrowConflictException_whenRatingAlreadyExists()
    {
        RatingRequestDto requestDto = new RatingRequestDto(VALID_RATING_VALUE);

        when(movieRepository.findById(TEST_MOVIE_ID)).thenReturn(Optional.of(testMovie));
        when(ratingRepository.existsByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID)).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> ratingService.createRating(TEST_MOVIE_ID, TEST_USER_ID, requestDto)
        );

        assertEquals(
                "Rating for movie " + TEST_MOVIE_ID + " already exists for current user",
                exception.getMessage()
        );

        verify(movieRepository).findById(TEST_MOVIE_ID);
        verify(ratingRepository).existsByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID);
        verifyNoInteractions(userRepository);
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void createRating_shouldThrowNotFoundException_whenUserDoesNotExist()
    {
        RatingRequestDto requestDto = new RatingRequestDto(VALID_RATING_VALUE);

        when(movieRepository.findById(TEST_MOVIE_ID)).thenReturn(Optional.of(testMovie));
        when(ratingRepository.existsByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> ratingService.createRating(TEST_MOVIE_ID, TEST_USER_ID, requestDto)
        );

        assertEquals("User with id " + TEST_USER_ID + " not found", exception.getMessage());

        verify(movieRepository).findById(TEST_MOVIE_ID);
        verify(ratingRepository).existsByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID);
        verify(userRepository).findById(TEST_USER_ID);
        verify(ratingRepository, never()).save(any());
    }

    @Test
    void updateRating_shouldUpdateRatingSuccessfully()
    {
        RatingRequestDto requestDto = new RatingRequestDto(UPDATED_RATING_VALUE);

        Rating existingRating = buildRating(TEST_RATING_ID, EXISTING_RATING_VALUE, testMovie, testUser);
        Rating updatedRating = buildRating(TEST_RATING_ID, UPDATED_RATING_VALUE, testMovie, testUser);

        when(movieRepository.existsById(TEST_MOVIE_ID)).thenReturn(true);
        when(ratingRepository.findByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID))
                .thenReturn(Optional.of(existingRating));
        when(ratingRepository.save(existingRating)).thenReturn(updatedRating);

        RatingResponseDto result = ratingService.updateRating(TEST_MOVIE_ID, TEST_USER_ID, requestDto);

        assertNotNull(result);
        assertEquals(TEST_RATING_ID, result.id());
        assertEquals(UPDATED_RATING_VALUE, result.value());
        assertEquals(TEST_MOVIE_ID, result.movieId());
        assertEquals(TEST_USER_ID, result.userId());

        assertEquals(UPDATED_RATING_VALUE, existingRating.getValue());

        verify(movieRepository).existsById(TEST_MOVIE_ID);
        verify(ratingRepository).findByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID);
        verify(ratingRepository).save(existingRating);
    }

    @Test
    void updateRating_shouldThrowNotFoundException_whenMovieDoesNotExist()
    {
        RatingRequestDto requestDto = new RatingRequestDto(UPDATED_RATING_VALUE);

        when(movieRepository.existsById(TEST_MOVIE_ID)).thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> ratingService.updateRating(TEST_MOVIE_ID, TEST_USER_ID, requestDto)
        );

        assertEquals("Movie with id " + TEST_MOVIE_ID + " not found", exception.getMessage());

        verify(movieRepository).existsById(TEST_MOVIE_ID);
        verifyNoMoreInteractions(ratingRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateRating_shouldThrowNotFoundException_whenRatingDoesNotExist()
    {
        RatingRequestDto requestDto = new RatingRequestDto(UPDATED_RATING_VALUE);

        when(movieRepository.existsById(TEST_MOVIE_ID)).thenReturn(true);
        when(ratingRepository.findByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> ratingService.updateRating(TEST_MOVIE_ID, TEST_USER_ID, requestDto)
        );

        assertEquals(
                "Rating for movie " + TEST_MOVIE_ID + " by current user not found",
                exception.getMessage()
        );

        verify(movieRepository).existsById(TEST_MOVIE_ID);
        verify(ratingRepository).findByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID);
        verify(ratingRepository, never()).save(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void deleteRating_shouldDeleteRatingSuccessfully()
    {
        Rating existingRating = buildRating(TEST_RATING_ID, EXISTING_RATING_VALUE, testMovie, testUser);

        when(movieRepository.existsById(TEST_MOVIE_ID)).thenReturn(true);
        when(ratingRepository.findByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID))
                .thenReturn(Optional.of(existingRating));

        assertDoesNotThrow(() -> ratingService.deleteRating(TEST_MOVIE_ID, TEST_USER_ID));

        verify(movieRepository).existsById(TEST_MOVIE_ID);
        verify(ratingRepository).findByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID);
        verify(ratingRepository).delete(existingRating);
    }

    @Test
    void deleteRating_shouldThrowNotFoundException_whenMovieDoesNotExist()
    {
        when(movieRepository.existsById(TEST_MOVIE_ID)).thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> ratingService.deleteRating(TEST_MOVIE_ID, TEST_USER_ID)
        );

        assertEquals("Movie with id " + TEST_MOVIE_ID + " not found", exception.getMessage());

        verify(movieRepository).existsById(TEST_MOVIE_ID);
        verifyNoMoreInteractions(ratingRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void deleteRating_shouldThrowNotFoundException_whenRatingDoesNotExist()
    {
        when(movieRepository.existsById(TEST_MOVIE_ID)).thenReturn(true);
        when(ratingRepository.findByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID))
                .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> ratingService.deleteRating(TEST_MOVIE_ID, TEST_USER_ID)
        );

        assertEquals(
                "Rating for movie " + TEST_MOVIE_ID + " by current user not found",
                exception.getMessage()
        );

        verify(movieRepository).existsById(TEST_MOVIE_ID);
        verify(ratingRepository).findByUser_IdAndMovie_Id(TEST_USER_ID, TEST_MOVIE_ID);
        verify(ratingRepository, never()).delete(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void getMyRatings_shouldReturnEmptyList_whenUserHasNoRatings()
    {
        when(ratingRepository.findAllByUser_Id(TEST_USER_ID)).thenReturn(List.of());

        List<UserRatingResponseDto> result = ratingService.getMyRatings(TEST_USER_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(ratingRepository).findAllByUser_Id(TEST_USER_ID);
        verifyNoInteractions(movieRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void getMyRatings_shouldReturnMappedRatings()
    {
        Movie movie2 = buildMovie(2L, "Interstellar");

        Rating rating1 = buildRating(TEST_RATING_ID, VALID_RATING_VALUE, testMovie, testUser);
        Rating rating2 = buildRating(101L, EXISTING_RATING_VALUE, movie2, testUser);

        when(ratingRepository.findAllByUser_Id(TEST_USER_ID))
                .thenReturn(List.of(rating1, rating2));

        List<UserRatingResponseDto> result = ratingService.getMyRatings(TEST_USER_ID);

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(TEST_MOVIE_ID, result.get(0).movieId());
        assertEquals("Inception", result.get(0).movieTitle());
        assertEquals(VALID_RATING_VALUE, result.get(0).value());

        assertEquals(2L, result.get(1).movieId());
        assertEquals("Interstellar", result.get(1).movieTitle());
        assertEquals(EXISTING_RATING_VALUE, result.get(1).value());

        verify(ratingRepository).findAllByUser_Id(TEST_USER_ID);
        verifyNoInteractions(movieRepository);
        verifyNoInteractions(userRepository);
    }

    // --- HELPER METHODS ---

    private Movie buildMovie(Long id, String title)
    {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setDescription("Test description");
        movie.setReleaseYear(2010);
        movie.setDurationMinutes(120);
        movie.setCountry("USA");
        return movie;
    }

    private User buildUser(Long id, String email)
    {
        User user = new User();
        user.setId(id);
        user.setName("Serhii");
        user.setEmail(email);
        user.setPasswordHash("hashed-password");
        return user;
    }

    private Rating buildRating(Long id, Integer value, Movie movie, User user)
    {
        Rating rating = new Rating();
        rating.setId(id);
        rating.setValue(value);
        rating.setMovie(movie);
        rating.setUser(user);
        rating.setCreatedAt(LocalDateTime.of(2026, 1, 1, 1, 1));
        rating.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 1, 2));
        return rating;
    }
}