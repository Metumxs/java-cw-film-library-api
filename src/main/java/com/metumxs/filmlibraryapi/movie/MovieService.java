package com.metumxs.filmlibraryapi.movie;

import com.metumxs.filmlibraryapi.domain.entity.Movie;
import com.metumxs.filmlibraryapi.domain.projection.MovieRatingSummaryProjection;
import com.metumxs.filmlibraryapi.domain.repository.MovieRepository;
import com.metumxs.filmlibraryapi.domain.repository.RatingRepository;
import com.metumxs.filmlibraryapi.exception.BadRequestException;
import com.metumxs.filmlibraryapi.exception.NotFoundException;
import com.metumxs.filmlibraryapi.movie.dto.MovieDetailsResponseDto;
import com.metumxs.filmlibraryapi.movie.dto.MovieSummaryResponseDto;
import com.metumxs.filmlibraryapi.movie.mapper.MovieMapper;
import com.metumxs.filmlibraryapi.domain.entity.Genre;
import com.metumxs.filmlibraryapi.domain.repository.GenreRepository;
import com.metumxs.filmlibraryapi.movie.dto.CreateMovieRequestDto;
import com.metumxs.filmlibraryapi.movie.dto.UpdateMovieRequestDto;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;
import java.util.Set;
import static com.metumxs.filmlibraryapi.validation.ValidationConstants.MOVIE_MIN_YEAR;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MovieService
{
    private final MovieRepository movieRepository;
    private final RatingRepository ratingRepository;
    private final MovieMapper movieMapper;
    private final GenreRepository genreRepository;

    public Page<MovieSummaryResponseDto> getMovies(
            int pageNumber,
            int pageSize,
            String title,
            String genre,
            Integer releaseYear
    )
    {
        validateReleaseYear(releaseYear);

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.ASC, "title")
        );

        Specification<Movie> specification = Specification.<Movie>unrestricted()
                .and(hasTitleContaining(title))
                .and(hasGenreName(genre))
                .and(hasReleaseYear(releaseYear));

        Page<Movie> moviePage = movieRepository.findAll(specification, pageable);

        List<Long> movieIds = moviePage.getContent()
                .stream()
                .map(Movie::getId)
                .toList();

        if (movieIds.isEmpty())
        {
            return moviePage.map(movie -> movieMapper
                    .toSummaryResponseDto(movie, null, 0L));
        }

        Map<Long, MovieRatingSummaryProjection> ratingSummaryByMovieId = ratingRepository
                .findRatingSummariesByMovieIds(movieIds)
                .stream()
                .collect(Collectors.toMap(
                        MovieRatingSummaryProjection::getMovieId,
                        Function.identity()
                ));

        return moviePage.map(movie ->
        {
            MovieRatingSummaryProjection movieRatingSummary = ratingSummaryByMovieId.get(movie.getId());

            Double averageRating = movieRatingSummary != null ? movieRatingSummary.getAverageRating() : null;
            Long ratingsCount = movieRatingSummary != null ? movieRatingSummary.getRatingsCount() : 0L;

            return movieMapper.toSummaryResponseDto(movie, averageRating, ratingsCount);
        });
    }

    public MovieDetailsResponseDto getMovieDetails(Long movieId)
    {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie with id " + movieId + " not found"));

        return mapToDetailsResponse(movie);
    }

    @Transactional
    public MovieDetailsResponseDto createMovie(CreateMovieRequestDto requestDto)
    {
        validateReleaseYear(requestDto.releaseYear());

        Set<Genre> genres = loadGenresByIds(requestDto.genreIds());

        Movie movie = movieMapper.toEntity(requestDto);
        movie.setGenres(genres);

        Movie savedMovie = movieRepository.save(movie);

        return mapToDetailsResponse(savedMovie);
    }

    @Transactional
    public MovieDetailsResponseDto updateMovie(Long movieId, UpdateMovieRequestDto requestDto)
    {
        validateReleaseYear(requestDto.releaseYear());

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie with id " + movieId + " not found"));

        Set<Genre> genres = loadGenresByIds(requestDto.genreIds());

        movieMapper.updateEntityFromDto(requestDto, movie);
        movie.setGenres(genres);

        return mapToDetailsResponse(movie);
    }

    @Transactional
    public void deleteMovie(Long movieId)
    {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie with id " + movieId + " not found"));

        movieRepository.delete(movie);
    }

    // --- HELPER METHODS ---

    private MovieDetailsResponseDto mapToDetailsResponse(Movie movie)
    {
        MovieRatingSummaryProjection movieRatingSummary = ratingRepository
                .findRatingSummaryByMovieId(movie.getId())
                .orElse(null);

        Double averageRating = movieRatingSummary != null ? movieRatingSummary.getAverageRating() : null;
        Long ratingsCount = movieRatingSummary != null ? movieRatingSummary.getRatingsCount() : 0L;

        return movieMapper.toDetailsResponseDto(movie, averageRating, ratingsCount);
    }

    private Set<Genre> loadGenresByIds(Set<Long> genreIds)
    {
        if (genreIds == null || genreIds.isEmpty())
        {
            throw new BadRequestException("genreIds must not be empty");
        }

        List<Genre> genres = genreRepository.findAllById(genreIds);

        if (genres.size() != genreIds.size())
        {
            throw new BadRequestException("One or more genreIds are invalid");
        }

        return new LinkedHashSet<>(genres);
    }

    private void validateReleaseYear(Integer releaseYear)
    {
        if (releaseYear == null)
        {
            return;
        }

        int currentYear = Year.now().getValue();

        if (releaseYear < MOVIE_MIN_YEAR || releaseYear > currentYear)
        {
            throw new BadRequestException(
                    "releaseYear must be between " + MOVIE_MIN_YEAR + " and " + currentYear
            );
        }
    }

    private Specification<Movie> hasTitleContaining(String title)
    {
        if (title == null || title.isBlank())
        {
            return Specification.unrestricted();
        }

        String normalizedTitle = "%" + title.trim().toLowerCase() + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        normalizedTitle
                );
    }

    private Specification<Movie> hasGenreName(String genre)
    {
        if (genre == null || genre.isBlank())
        {
            return Specification.unrestricted();
        }

        String normalizedGenre = genre.trim().toLowerCase();

        return (root, query, criteriaBuilder) ->
        {
            if (query != null)
            {
                query.distinct(true);
            }

            Join<Object, Object> genresJoin = root.join("genres");

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(genresJoin.get("name")),
                    normalizedGenre
            );
        };
    }

    private Specification<Movie> hasReleaseYear(Integer releaseYear)
    {
        if (releaseYear == null)
        {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("releaseYear"), releaseYear
                );
    }
}