package com.metumxs.filmlibraryapi.movie.service;

import com.metumxs.filmlibraryapi.domain.entity.Movie;
import com.metumxs.filmlibraryapi.domain.projection.MovieRatingSummaryProjection;
import com.metumxs.filmlibraryapi.domain.repository.GenreRepository;
import com.metumxs.filmlibraryapi.domain.repository.MovieRepository;
import com.metumxs.filmlibraryapi.domain.repository.RatingRepository;
import com.metumxs.filmlibraryapi.exception.BadRequestException;
import com.metumxs.filmlibraryapi.exception.NotFoundException;
import com.metumxs.filmlibraryapi.movie.dto.MovieDetailsResponseDto;
import com.metumxs.filmlibraryapi.movie.dto.MovieSummaryResponseDto;
import com.metumxs.filmlibraryapi.movie.mapper.MovieMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest
{
    @Mock
    private MovieRepository movieRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private MovieMapper movieMapper;

    @Mock
    private GenreRepository genreRepository;

    private MovieService movieService;

    @BeforeEach
    void setUp()
    {
        movieService = new MovieService(movieRepository, ratingRepository, movieMapper,  genreRepository);
    }

    @Test
    void getMovies_shouldReturnEmptyPage_whenNoMoviesFound()
    {
        Page<Movie> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(movieRepository.findAll(
                ArgumentMatchers.<Specification<Movie>>any(),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        Page<MovieSummaryResponseDto> result = movieService.getMovies(0, 20, null, null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(movieRepository).findAll(
                ArgumentMatchers.<Specification<Movie>>any(),
                any(Pageable.class)
        );
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void getMovies_shouldReturnMappedMoviePageWithRatings()
    {
        Movie movie = buildMovie(1L, "The Dark Knight", 2008);

        Page<Movie> moviePage = new PageImpl<>(
                List.of(movie),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "title")),
                1
        );

        MovieRatingSummaryProjection ratingSummaryProjection = new TestMovieRatingSummaryProjection(1L, 9.2, 1250L);

        MovieSummaryResponseDto responseDto = new MovieSummaryResponseDto(
                1L,
                "The Dark Knight",
                2008,
                Set.of("Action", "Drama"),
                9.2,
                1250L
        );

        when(movieRepository.findAll(
                ArgumentMatchers.<Specification<Movie>>any(),
                any(Pageable.class)
        )).thenReturn(moviePage);

        when(ratingRepository.findRatingSummariesByMovieIds(List.of(1L)))
                .thenReturn(List.of(ratingSummaryProjection));

        when(movieMapper.toSummaryResponseDto(movie, 9.2, 1250L))
                .thenReturn(responseDto);

        Page<MovieSummaryResponseDto> result = movieService.getMovies(0, 20, "dark", "Drama", 2008);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("The Dark Knight", result.getContent().getFirst().title());
        assertEquals(9.2, result.getContent().getFirst().averageRating());
        assertEquals(1250L, result.getContent().getFirst().ratingsCount());

        verify(movieRepository).findAll(
                ArgumentMatchers.<Specification<Movie>>any(),
                any(Pageable.class)
        );
        verify(ratingRepository).findRatingSummariesByMovieIds(List.of(1L));
        verify(movieMapper).toSummaryResponseDto(movie, 9.2, 1250L);
    }

    @Test
    void getMovies_shouldReturnMappedMoviePageWithNullRatingAndZeroCount_whenNoRatingSummaryExists()
    {
        Movie movie = buildMovie(1L, "Interstellar", 2014);

        Page<Movie> moviePage = new PageImpl<>(
                List.of(movie),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "title")),
                1
        );

        MovieSummaryResponseDto responseDto = new MovieSummaryResponseDto(
                1L,
                "Interstellar",
                2014,
                Set.of("Drama", "Sci-Fi"),
                null,
                0L
        );

        when(movieRepository.findAll(
                ArgumentMatchers.<Specification<Movie>>any(),
                any(Pageable.class)
        )).thenReturn(moviePage);

        when(ratingRepository.findRatingSummariesByMovieIds(List.of(1L)))
                .thenReturn(List.of());

        when(movieMapper.toSummaryResponseDto(movie, null, 0L))
                .thenReturn(responseDto);

        Page<MovieSummaryResponseDto> result = movieService.getMovies(0, 20, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Interstellar", result.getContent().getFirst().title());
        assertNull(result.getContent().getFirst().averageRating());
        assertEquals(0L, result.getContent().getFirst().ratingsCount());

        verify(ratingRepository).findRatingSummariesByMovieIds(List.of(1L));
        verify(movieMapper).toSummaryResponseDto(movie, null, 0L);
    }

    @Test
    void getMovies_shouldThrowBadRequestException_whenReleaseYearIsLessThan1888()
    {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> movieService.getMovies(0, 20, null, null, 1887)
        );

        assertTrue(exception.getMessage().contains("releaseYear must be between 1888"));

        verifyNoInteractions(movieRepository);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void getMovies_shouldThrowBadRequestException_whenReleaseYearIsGreaterThanCurrentYear()
    {
        int invalidFutureYear = Year.now().getValue() + 1;

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> movieService.getMovies(0, 20, null, null, invalidFutureYear)
        );

        assertTrue(exception.getMessage().contains("releaseYear must be between 1888"));

        verifyNoInteractions(movieRepository);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void getMovieDetails_shouldReturnMovieDetailsWithRatingSummary()
    {
        Movie movie = buildMovie(1L, "Inception", 2010);
        MovieRatingSummaryProjection ratingSummaryProjection = new TestMovieRatingSummaryProjection(1L, 8.8, 980L);

        MovieDetailsResponseDto responseDto = new MovieDetailsResponseDto(
                1L,
                "Inception",
                "Dream infiltration thriller",
                2010,
                148,
                "USA",
                Set.of("Sci-Fi", "Thriller"),
                8.8,
                980L
        );

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(ratingRepository.findRatingSummaryByMovieId(1L)).thenReturn(Optional.of(ratingSummaryProjection));
        when(movieMapper.toDetailsResponseDto(movie, 8.8, 980L)).thenReturn(responseDto);

        MovieDetailsResponseDto result = movieService.getMovieDetails(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Inception", result.title());
        assertEquals(8.8, result.averageRating());
        assertEquals(980L, result.ratingsCount());

        verify(movieRepository).findById(1L);
        verify(ratingRepository).findRatingSummaryByMovieId(1L);
        verify(movieMapper).toDetailsResponseDto(movie, 8.8, 980L);
    }

    @Test
    void getMovieDetails_shouldReturnMovieDetailsWithNullRatingAndZeroCount_whenNoRatingSummaryExists()
    {
        Movie movie = buildMovie(2L, "Parasite", 2019);

        MovieDetailsResponseDto responseDto = new MovieDetailsResponseDto(
                2L,
                "Parasite",
                "A poor family infiltrates a wealthy household",
                2019,
                132,
                "South Korea",
                Set.of("Drama", "Thriller"),
                null,
                0L
        );

        when(movieRepository.findById(2L)).thenReturn(Optional.of(movie));
        when(ratingRepository.findRatingSummaryByMovieId(2L)).thenReturn(Optional.empty());
        when(movieMapper.toDetailsResponseDto(movie, null, 0L)).thenReturn(responseDto);

        MovieDetailsResponseDto result = movieService.getMovieDetails(2L);

        assertNotNull(result);
        assertEquals(2L, result.id());
        assertNull(result.averageRating());
        assertEquals(0L, result.ratingsCount());

        verify(movieRepository).findById(2L);
        verify(ratingRepository).findRatingSummaryByMovieId(2L);
        verify(movieMapper).toDetailsResponseDto(movie, null, 0L);
    }

    @Test
    void getMovieDetails_shouldThrowNotFoundException_whenMovieDoesNotExist()
    {
        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> movieService.getMovieDetails(999L)
        );

        assertEquals("Movie with id 999 not found", exception.getMessage());

        verify(movieRepository).findById(999L);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    private Movie buildMovie(Long id, String title, Integer releaseYear)
    {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setDescription("Test description");
        movie.setReleaseYear(releaseYear);
        movie.setDurationMinutes(120);
        movie.setCountry("USA");
        return movie;
    }

    private record TestMovieRatingSummaryProjection(
            Long getMovieId,
            Double getAverageRating,
            Long getRatingsCount
    ) implements MovieRatingSummaryProjection {}
}