package com.metumxs.filmlibraryapi.movie.controller;

import com.metumxs.filmlibraryapi.movie.dto.MovieDetailsResponseDto;
import com.metumxs.filmlibraryapi.movie.dto.MovieSummaryResponseDto;
import com.metumxs.filmlibraryapi.movie.service.MovieService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@Validated
public class MovieController
{
    private final MovieService movieService;

    @GetMapping
    public ResponseEntity<Page<MovieSummaryResponseDto>> getMovies(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be greater than or equal to 0")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be greater than or equal to 1")
            @Max(value = 100, message = "size must be less than or equal to 100")
            int size,

            @RequestParam(required = false)
            String title,

            @RequestParam(required = false)
            String genre,

            @RequestParam(required = false)
            Integer releaseYear
    )
    {
        return ResponseEntity.ok(
                movieService.getMovies(page, size, title, genre, releaseYear)
        );
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<MovieDetailsResponseDto> getMovieDetails(
            @PathVariable
            @Positive(message = "movieId must be greater than 0")
            Long movieId
    )
    {
        return ResponseEntity.ok(movieService.getMovieDetails(movieId));
    }
}