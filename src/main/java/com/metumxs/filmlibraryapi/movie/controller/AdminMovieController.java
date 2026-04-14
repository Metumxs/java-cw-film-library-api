package com.metumxs.filmlibraryapi.movie.controller;

import com.metumxs.filmlibraryapi.movie.dto.CreateMovieRequestDto;
import com.metumxs.filmlibraryapi.movie.dto.MovieDetailsResponseDto;
import com.metumxs.filmlibraryapi.movie.dto.UpdateMovieRequestDto;
import com.metumxs.filmlibraryapi.movie.service.MovieService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/movies")
@RequiredArgsConstructor
@Validated
public class AdminMovieController
{
    private final MovieService movieService;

    @PostMapping
    public ResponseEntity<MovieDetailsResponseDto> createMovie(
            @Valid @RequestBody CreateMovieRequestDto requestDto
    )
    {
        MovieDetailsResponseDto responseDto = movieService.createMovie(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

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