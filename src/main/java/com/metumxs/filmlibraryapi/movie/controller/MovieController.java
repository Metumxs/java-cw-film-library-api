package com.metumxs.filmlibraryapi.movie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.metumxs.filmlibraryapi.exception.ErrorResponse;
import com.metumxs.filmlibraryapi.movie.dto.MovieDetailsResponseDto;
import com.metumxs.filmlibraryapi.movie.dto.MovieSummaryResponseDto;
import com.metumxs.filmlibraryapi.movie.MovieService;
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
@Tag(name = "Movies", description = "Public movie catalog endpoints")
public class MovieController
{
    private final MovieService movieService;

    @Operation(summary = "Get paginated movie catalog with optional filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movies retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(summary = "Get movie details by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movie details retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid movie id", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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