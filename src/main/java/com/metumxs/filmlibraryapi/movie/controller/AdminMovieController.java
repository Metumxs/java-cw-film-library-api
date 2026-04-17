package com.metumxs.filmlibraryapi.movie.controller;

import com.metumxs.filmlibraryapi.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.metumxs.filmlibraryapi.movie.dto.CreateMovieRequestDto;
import com.metumxs.filmlibraryapi.movie.dto.MovieDetailsResponseDto;
import com.metumxs.filmlibraryapi.movie.dto.UpdateMovieRequestDto;
import com.metumxs.filmlibraryapi.movie.service.MovieService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/movies")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Movies", description = "Administrative movie management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminMovieController
{
    private final MovieService movieService;

    @Operation(summary = "Create a new movie (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Movie created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<MovieDetailsResponseDto> createMovie(
            @Valid @RequestBody CreateMovieRequestDto requestDto
    )
    {
        MovieDetailsResponseDto responseDto = movieService.createMovie(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Operation(summary = "Update an existing movie (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movie updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{movieId}")
    public ResponseEntity<MovieDetailsResponseDto> updateMovie(
            @PathVariable
            @Positive(message = "movieId must be greater than 0")
            Long movieId,

            @Valid @RequestBody UpdateMovieRequestDto requestDto
    )
    {
        return ResponseEntity.ok(movieService.updateMovie(movieId, requestDto));
    }

    @Operation(summary = "Delete a movie (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Movie deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Movie not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> deleteMovie(
            @PathVariable
            @Positive(message = "movieId must be greater than 0")
            Long movieId
    )
    {
        movieService.deleteMovie(movieId);

        return ResponseEntity.noContent().build();
    }
}