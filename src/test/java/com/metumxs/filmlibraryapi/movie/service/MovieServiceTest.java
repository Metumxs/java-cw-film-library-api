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

        MovieRatingSummaryProjection movieRatingSummary = new TestMovieRatingSummaryProjection(1L, 9.2, 1250L);

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
                .thenReturn(List.of(movieRatingSummary));

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
        Movie movie = buildMovie(1L, "Inception", 2010);
        MovieRatingSummaryProjection movieRatingSummary = new TestMovieRatingSummaryProjection(1L, 8.8, 980L);

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
        when(ratingRepository.findRatingSummaryByMovieId(1L)).thenReturn(Optional.of(movieRatingSummary));
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

        Genre drama = buildGenre(1L, "Drama");
        Genre mystery = buildGenre(2L, "Mystery");

        Movie movieToSave = new Movie();
        Movie savedMovie = buildMovie(10L, "The Prestige", 2006);
        savedMovie.setGenres(Set.of(drama, mystery));

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
                .thenReturn(List.of(drama, mystery));
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

        Genre drama = buildGenre(1L, "Drama");

        when(genreRepository.findAllById(requestDto.genreIds()))
                .thenReturn(List.of(drama));

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

        Movie existingMovie = buildMovie(5L, "Old Title", 2005);

        Genre drama = buildGenre(1L, "Drama");
        Genre thriller = buildGenre(2L, "Thriller");

        Movie updatedMovie = buildMovie(5L, "Updated Title", 2010);
        updatedMovie.setGenres(Set.of(drama, thriller));

        MovieDetailsResponseDto responseDto = new MovieDetailsResponseDto(
                5L,
                "Updated Title",
                "Updated description",
                2010,
                140,
                "USA",
                Set.of("Drama", "Thriller"),
                null,
                0L
        );

        when(movieRepository.findById(5L)).thenReturn(Optional.of(existingMovie));
        when(genreRepository.findAllById(requestDto.genreIds()))
                .thenReturn(List.of(drama, thriller));
        when(movieRepository.save(existingMovie)).thenReturn(updatedMovie);
        when(ratingRepository.findRatingSummaryByMovieId(5L)).thenReturn(Optional.empty());
        when(movieMapper.toDetailsResponseDto(updatedMovie, null, 0L)).thenReturn(responseDto);

        MovieDetailsResponseDto result = movieService.updateMovie(5L, requestDto);
        assertNotNull(result);
        assertEquals(5L, result.id());
        assertEquals("Updated Title", result.title());

        verify(movieRepository).findById(5L);
        verify(genreRepository).findAllById(requestDto.genreIds());
        verify(movieMapper).updateEntityFromDto(requestDto, existingMovie);
        verify(movieRepository).save(existingMovie);
        verify(ratingRepository).findRatingSummaryByMovieId(5L);
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

        when(movieRepository.findById(999L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> movieService.updateMovie(999L, requestDto)
        );

        assertEquals("Movie with id 999 not found", exception.getMessage());

        verify(movieRepository).findById(999L);
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

        Movie existingMovie = buildMovie(5L, "Old Title", 2005);
        Genre drama = buildGenre(1L, "Drama");

        when(movieRepository.findById(5L)).thenReturn(Optional.of(existingMovie));
        when(genreRepository.findAllById(requestDto.genreIds()))
                .thenReturn(List.of(drama));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> movieService.updateMovie(5L, requestDto)
        );

        assertEquals("One or more genreIds are invalid", exception.getMessage());

        verify(movieRepository).findById(5L);
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
                () -> movieService.updateMovie(5L, requestDto)
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
        Movie movie = buildMovie(7L, "Interstellar", 2014);

        when(movieRepository.findById(7L)).thenReturn(Optional.of(movie));

        assertDoesNotThrow(() -> movieService.deleteMovie(7L));

        verify(movieRepository).findById(7L);
        verify(movieRepository).delete(movie);
        verifyNoInteractions(genreRepository);
        verifyNoInteractions(ratingRepository);
        verifyNoInteractions(movieMapper);
    }

    @Test
    void deleteMovie_shouldThrowNotFoundException_whenMovieDoesNotExist()
    {
        when(movieRepository.findById(777L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> movieService.deleteMovie(777L)
        );

        assertEquals("Movie with id 777 not found", exception.getMessage());

        verify(movieRepository).findById(777L);
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