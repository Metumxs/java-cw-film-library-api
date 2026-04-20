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
import com.metumxs.filmlibraryapi.domain.entity.Genre;
import com.metumxs.filmlibraryapi.movie.dto.CreateMovieRequestDto;
import com.metumxs.filmlibraryapi.movie.dto.UpdateMovieRequestDto;
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
import static com.metumxs.filmlibraryapi.validation.ValidationConstants.MOVIE_MIN_YEAR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest
{
    private static final Long TEST_MOVIE_ID = 1L;
    private static final String TEST_MOVIE_TITLE = "The Dark Knight";
    private static final Integer TEST_MOVIE_YEAR = 2008;
    private static final Double TEST_AVERAGE_RATING = 9.2;
    private static final Long TEST_RATINGS_COUNT = 1250L;
    private static final Long NON_EXISTENT_MOVIE_ID = 999999999999L;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private MovieMapper movieMapper;

    @Mock
    private GenreRepository genreRepository;

    private MovieService movieService;

    private Movie testMovie;
    private Genre dramaGenre;
    private Genre mysteryGenre;

    @BeforeEach
    void setUp()
    {
        movieService = new MovieService(movieRepository, ratingRepository, movieMapper, genreRepository);

        testMovie = buildMovie(TEST_MOVIE_ID, TEST_MOVIE_TITLE, TEST_MOVIE_YEAR);

        dramaGenre = buildGenre(1L, "Drama");
        mysteryGenre = buildGenre(2L, "Mystery");
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
        Page<Movie> moviePage = new PageImpl<>(
                List.of(testMovie),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "title")),
                1
        );

        MovieRatingSummaryProjection movieRatingSummary = new TestMovieRatingSummaryProjection(TEST_MOVIE_ID, TEST_AVERAGE_RATING, TEST_RATINGS_COUNT);

        MovieSummaryResponseDto responseDto = new MovieSummaryResponseDto(
                TEST_MOVIE_ID,
                TEST_MOVIE_TITLE,
                TEST_MOVIE_YEAR,
                Set.of("Action", "Drama"),
                TEST_AVERAGE_RATING,
                TEST_RATINGS_COUNT
        );

        when(movieRepository.findAll(
                ArgumentMatchers.<Specification<Movie>>any(),
                any(Pageable.class)
        )).thenReturn(moviePage);

        when(ratingRepository.findRatingSummariesByMovieIds(List.of(TEST_MOVIE_ID)))
                .thenReturn(List.of(movieRatingSummary));

        when(movieMapper.toSummaryResponseDto(testMovie, TEST_AVERAGE_RATING, TEST_RATINGS_COUNT))
                .thenReturn(responseDto);

        Page<MovieSummaryResponseDto> result = movieService.getMovies(0, 20, "dark", "Drama", TEST_MOVIE_YEAR);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(TEST_MOVIE_TITLE, result.getContent().getFirst().title());
        assertEquals(TEST_AVERAGE_RATING, result.getContent().getFirst().averageRating());
        assertEquals(TEST_RATINGS_COUNT, result.getContent().getFirst().ratingsCount());

        verify(movieRepository).findAll(
                ArgumentMatchers.<Specification<Movie>>any(),
                any(Pageable.class)
        );
        verify(ratingRepository).findRatingSummariesByMovieIds(List.of(TEST_MOVIE_ID));
        verify(movieMapper).toSummaryResponseDto(testMovie, TEST_AVERAGE_RATING, TEST_RATINGS_COUNT);
    }

    @Test
    void getMovies_shouldReturnMappedMoviePageWithNullRatingAndZeroCount_whenNoRatingSummaryExists()
    {
        Page<Movie> moviePage = new PageImpl<>(
                List.of(testMovie),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "title")),
                1
        );

        MovieSummaryResponseDto responseDto = new MovieSummaryResponseDto(
                TEST_MOVIE_ID,
                TEST_MOVIE_TITLE,
                TEST_MOVIE_YEAR,
                Set.of("Drama", "Sci-Fi"),
                null,
                0L
        );

        when(movieRepository.findAll(
                ArgumentMatchers.<Specification<Movie>>any(),
                any(Pageable.class)
        )).thenReturn(moviePage);

        when(ratingRepository.findRatingSummariesByMovieIds(List.of(TEST_MOVIE_ID)))
                .thenReturn(List.of());

        when(movieMapper.toSummaryResponseDto(testMovie, null, 0L))
                .thenReturn(responseDto);

        Page<MovieSummaryResponseDto> result = movieService.getMovies(0, 20, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(TEST_MOVIE_TITLE, result.getContent().getFirst().title());
        assertNull(result.getContent().getFirst().averageRating());
        assertEquals(0L, result.getContent().getFirst().ratingsCount());

        verify(ratingRepository).findRatingSummariesByMovieIds(List.of(TEST_MOVIE_ID));
        verify(movieMapper).toSummaryResponseDto(testMovie, null, 0L);
    }

    @Test
    void getMovies_shouldThrowBadRequestException_whenReleaseYearIsLessThan1888()
    {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> movieService.getMovies(0, 20, null, null, MOVIE_MIN_YEAR - 1)
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

        assertTrue(exception.getMessage().contains("releaseYear must be between " + MOVIE_MIN_YEAR));

        verifyNoInteractions(movieRepository);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void getMovieDetails_shouldReturnMovieDetailsWithRatingSummary()
    {
        MovieRatingSummaryProjection movieRatingSummary = new TestMovieRatingSummaryProjection(TEST_MOVIE_ID, TEST_AVERAGE_RATING, TEST_RATINGS_COUNT);

        MovieDetailsResponseDto responseDto = new MovieDetailsResponseDto(
                TEST_MOVIE_ID,
                TEST_MOVIE_TITLE,
                "Test description",
                TEST_MOVIE_YEAR,
                120,
                "USA",
                Set.of("Action", "Drama"),
                TEST_AVERAGE_RATING,
                TEST_RATINGS_COUNT
        );

        when(movieRepository.findById(TEST_MOVIE_ID)).thenReturn(Optional.of(testMovie));
        when(ratingRepository.findRatingSummaryByMovieId(TEST_MOVIE_ID)).thenReturn(Optional.of(movieRatingSummary));
        when(movieMapper.toDetailsResponseDto(testMovie, TEST_AVERAGE_RATING, TEST_RATINGS_COUNT)).thenReturn(responseDto);

        MovieDetailsResponseDto result = movieService.getMovieDetails(TEST_MOVIE_ID);

        assertNotNull(result);
        assertEquals(TEST_MOVIE_ID, result.id());
        assertEquals(TEST_MOVIE_TITLE, result.title());
        assertEquals(TEST_AVERAGE_RATING, result.averageRating());
        assertEquals(TEST_RATINGS_COUNT, result.ratingsCount());

        verify(movieRepository).findById(TEST_MOVIE_ID);
        verify(ratingRepository).findRatingSummaryByMovieId(TEST_MOVIE_ID);
        verify(movieMapper).toDetailsResponseDto(testMovie, TEST_AVERAGE_RATING, TEST_RATINGS_COUNT);
    }

    @Test
    void getMovieDetails_shouldReturnMovieDetailsWithNullRatingAndZeroCount_whenNoRatingSummaryExists()
    {
        MovieDetailsResponseDto responseDto = new MovieDetailsResponseDto(
                TEST_MOVIE_ID,
                TEST_MOVIE_TITLE,
                "Test description",
                TEST_MOVIE_YEAR,
                120,
                "USA",
                Set.of("Drama", "Sci-Fi"),
                null,
                0L
        );

        when(movieRepository.findById(TEST_MOVIE_ID)).thenReturn(Optional.of(testMovie));
        when(ratingRepository.findRatingSummaryByMovieId(TEST_MOVIE_ID)).thenReturn(Optional.empty());
        when(movieMapper.toDetailsResponseDto(testMovie, null, 0L)).thenReturn(responseDto);

        MovieDetailsResponseDto result = movieService.getMovieDetails(TEST_MOVIE_ID);

        assertNotNull(result);
        assertEquals(TEST_MOVIE_ID, result.id());
        assertNull(result.averageRating());
        assertEquals(0L, result.ratingsCount());

        verify(movieRepository).findById(TEST_MOVIE_ID);
        verify(ratingRepository).findRatingSummaryByMovieId(TEST_MOVIE_ID);
        verify(movieMapper).toDetailsResponseDto(testMovie, null, 0L);
    }

    @Test
    void getMovieDetails_shouldThrowNotFoundException_whenMovieDoesNotExist()
    {
        when(movieRepository.findById(NON_EXISTENT_MOVIE_ID)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> movieService.getMovieDetails(NON_EXISTENT_MOVIE_ID)
        );

        assertEquals("Movie with id " + NON_EXISTENT_MOVIE_ID + " not found", exception.getMessage());

        verify(movieRepository).findById(NON_EXISTENT_MOVIE_ID);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void createMovie_shouldCreateMovieSuccessfully()
    {
        CreateMovieRequestDto requestDto = new CreateMovieRequestDto(
                "The Prestige",
                "Two rival magicians engage in a dangerous battle of obsession.",
                2006,
                130,
                "USA",
                Set.of(1L, 2L)
        );

        Movie movieToSave = new Movie();
        Movie savedMovie = buildMovie(10L, "The Prestige", 2006);
        savedMovie.setGenres(Set.of(dramaGenre, mysteryGenre));

        MovieDetailsResponseDto responseDto = new MovieDetailsResponseDto(
                10L,
                "The Prestige",
                "Two rival magicians engage in a dangerous battle of obsession.",
                2006,
                130,
                "USA",
                Set.of("Drama", "Mystery"),
                null,
                0L
        );

        when(genreRepository.findAllById(requestDto.genreIds()))
                .thenReturn(List.of(dramaGenre, mysteryGenre));
        when(movieMapper.toEntity(requestDto))
                .thenReturn(movieToSave);
        when(movieRepository.save(movieToSave))
                .thenReturn(savedMovie);
        when(ratingRepository.findRatingSummaryByMovieId(10L))
                .thenReturn(Optional.empty());
        when(movieMapper.toDetailsResponseDto(savedMovie, null, 0L))
                .thenReturn(responseDto);

        MovieDetailsResponseDto result = movieService.createMovie(requestDto);

        assertNotNull(result);
        assertEquals(10L, result.id());
        assertEquals("The Prestige", result.title());
        assertNull(result.averageRating());
        assertEquals(0L, result.ratingsCount());

        assertEquals(2, movieToSave.getGenres().size());

        verify(genreRepository).findAllById(requestDto.genreIds());
        verify(movieMapper).toEntity(requestDto);
        verify(movieRepository).save(movieToSave);
        verify(ratingRepository).findRatingSummaryByMovieId(10L);
        verify(movieMapper).toDetailsResponseDto(savedMovie, null, 0L);
    }

    @Test
    void createMovie_shouldThrowBadRequestException_whenGenreIdsAreEmpty()
    {
        CreateMovieRequestDto requestDto = new CreateMovieRequestDto(
                "The Prestige",
                "Two rival magicians engage in a dangerous battle of obsession.",
                2006,
                130,
                "USA",
                Set.of()
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> movieService.createMovie(requestDto)
        );

        assertEquals("genreIds must not be empty", exception.getMessage());

        verifyNoInteractions(genreRepository);
        verifyNoInteractions(movieRepository);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void createMovie_shouldThrowBadRequestException_whenSomeGenreIdsAreInvalid()
    {
        CreateMovieRequestDto requestDto = new CreateMovieRequestDto(
                "The Prestige",
                "Two rival magicians engage in a dangerous battle of obsession.",
                2006,
                130,
                "USA",
                Set.of(1L, 2L)
        );

        when(genreRepository.findAllById(requestDto.genreIds()))
                .thenReturn(List.of(dramaGenre));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> movieService.createMovie(requestDto)
        );

        assertEquals("One or more genreIds are invalid", exception.getMessage());

        verify(genreRepository).findAllById(requestDto.genreIds());
        verifyNoInteractions(movieRepository);
        verifyNoInteractions(ratingRepository);
        verify(movieMapper, never()).toEntity(any());
    }

    @Test
    void createMovie_shouldThrowBadRequestException_whenReleaseYearIsInvalid()
    {
        CreateMovieRequestDto requestDto = new CreateMovieRequestDto(
                "The Prestige",
                "Two rival magicians engage in a dangerous battle of obsession.",
                1800,
                130,
                "USA",
                Set.of(1L)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> movieService.createMovie(requestDto)
        );

        assertTrue(exception.getMessage().contains("releaseYear must be between " + MOVIE_MIN_YEAR));

        verifyNoInteractions(genreRepository);
        verifyNoInteractions(movieRepository);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void updateMovie_shouldUpdateMovieSuccessfully()
    {
        UpdateMovieRequestDto requestDto = new UpdateMovieRequestDto(
                "Updated Title",
                "Updated description",
                2010,
                140,
                "USA",
                Set.of(1L, 2L)
        );

        Movie updatedMovie = buildMovie(TEST_MOVIE_ID, "Updated Title", 2010);
        updatedMovie.setGenres(Set.of(dramaGenre, mysteryGenre));

        MovieDetailsResponseDto responseDto = new MovieDetailsResponseDto(
                TEST_MOVIE_ID,
                "Updated Title",
                "Updated description",
                2010,
                140,
                "USA",
                Set.of("Drama", "Mystery"),
                null,
                0L
        );

        when(movieRepository.findById(TEST_MOVIE_ID)).thenReturn(Optional.of(testMovie));
        when(genreRepository.findAllById(requestDto.genreIds()))
                .thenReturn(List.of(dramaGenre, mysteryGenre));
        when(ratingRepository.findRatingSummaryByMovieId(TEST_MOVIE_ID)).thenReturn(Optional.empty());
        when(movieMapper.toDetailsResponseDto(updatedMovie, null, 0L)).thenReturn(responseDto);

        MovieDetailsResponseDto result = movieService.updateMovie(TEST_MOVIE_ID, requestDto);
        assertNotNull(result);
        assertEquals(TEST_MOVIE_ID, result.id());
        assertEquals("Updated Title", result.title());

        verify(movieRepository).findById(TEST_MOVIE_ID);
        verify(genreRepository).findAllById(requestDto.genreIds());
        verify(movieMapper).updateEntityFromDto(requestDto, testMovie);
        verify(ratingRepository).findRatingSummaryByMovieId(TEST_MOVIE_ID);
        verify(movieMapper).toDetailsResponseDto(updatedMovie, null, 0L);
    }

    @Test
    void updateMovie_shouldThrowNotFoundException_whenMovieDoesNotExist()
    {
        UpdateMovieRequestDto requestDto = new UpdateMovieRequestDto(
                "Updated Title",
                "Updated description",
                2010,
                140,
                "USA",
                Set.of(1L)
        );

        when(movieRepository.findById(NON_EXISTENT_MOVIE_ID)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> movieService.updateMovie(NON_EXISTENT_MOVIE_ID, requestDto)
        );

        assertEquals("Movie with id " + NON_EXISTENT_MOVIE_ID + " not found", exception.getMessage());

        verify(movieRepository).findById(NON_EXISTENT_MOVIE_ID);
        verifyNoInteractions(genreRepository);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void updateMovie_shouldThrowBadRequestException_whenGenreIdsAreInvalid()
    {
        UpdateMovieRequestDto requestDto = new UpdateMovieRequestDto(
                "Updated Title",
                "Updated description",
                2010,
                140,
                "USA",
                Set.of(1L, 2L)
        );

        when(movieRepository.findById(TEST_MOVIE_ID)).thenReturn(Optional.of(testMovie));
        when(genreRepository.findAllById(requestDto.genreIds()))
                .thenReturn(List.of(dramaGenre));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> movieService.updateMovie(TEST_MOVIE_ID, requestDto)
        );

        assertEquals("One or more genreIds are invalid", exception.getMessage());

        verify(movieRepository).findById(TEST_MOVIE_ID);
        verify(genreRepository).findAllById(requestDto.genreIds());
        verify(movieMapper, never()).updateEntityFromDto(any(), any());
        verify(movieRepository, never()).save(any());
        verifyNoInteractions(ratingRepository);
    }

    @Test
    void updateMovie_shouldThrowBadRequestException_whenReleaseYearIsInvalid()
    {
        UpdateMovieRequestDto requestDto = new UpdateMovieRequestDto(
                "Updated Title",
                "Updated description",
                Year.now().getValue() + 1,
                140,
                "USA",
                Set.of(1L)
        );

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> movieService.updateMovie(TEST_MOVIE_ID, requestDto)
        );

        assertTrue(exception.getMessage().contains("releaseYear must be between " + MOVIE_MIN_YEAR));

        verifyNoInteractions(movieRepository);
        verifyNoInteractions(genreRepository);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void deleteMovie_shouldDeleteMovieSuccessfully()
    {
        when(movieRepository.findById(TEST_MOVIE_ID)).thenReturn(Optional.of(testMovie));

        assertDoesNotThrow(() -> movieService.deleteMovie(TEST_MOVIE_ID));

        verify(movieRepository).findById(TEST_MOVIE_ID);
        verify(movieRepository).delete(testMovie);
        verifyNoInteractions(genreRepository);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void deleteMovie_shouldThrowNotFoundException_whenMovieDoesNotExist()
    {
        when(movieRepository.findById(NON_EXISTENT_MOVIE_ID)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> movieService.deleteMovie(NON_EXISTENT_MOVIE_ID)
        );

        assertEquals("Movie with id " + NON_EXISTENT_MOVIE_ID + " not found", exception.getMessage());

        verify(movieRepository).findById(NON_EXISTENT_MOVIE_ID);
        verify(movieRepository, never()).delete(any(Movie.class));
        verifyNoInteractions(genreRepository);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    // HELPER METHODS

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

    private Genre buildGenre(Long id, String name)
    {
        Genre genre = new Genre();

        genre.setId(id);
        genre.setName(name);

        return genre;
    }

    private record TestMovieRatingSummaryProjection(
            Long getMovieId,
            Double getAverageRating,
            Long getRatingsCount
    ) implements MovieRatingSummaryProjection {}
}