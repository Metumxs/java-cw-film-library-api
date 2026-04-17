package com.metumxs.filmlibraryapi.rating.controller;

import com.metumxs.filmlibraryapi.rating.dto.RatingRequestDto;
import com.metumxs.filmlibraryapi.rating.dto.RatingResponseDto;
import com.metumxs.filmlibraryapi.rating.dto.UserRatingResponseDto;
import com.metumxs.filmlibraryapi.rating.service.RatingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class RatingController
{
    private final RatingService ratingService;

    @PostMapping("/api/v1/movies/{movieId}/rating")
    public ResponseEntity<RatingResponseDto> createRating(
            @PathVariable
            @Positive(message = "movieId must be greater than 0")
            Long movieId,

            @Valid @RequestBody RatingRequestDto requestDto,
            JwtAuthenticationToken authentication
    )
    {
        Long currentUserId = authentication.getToken().getClaim("userId");

        RatingResponseDto responseDto = ratingService.createRating(movieId, currentUserId, requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PutMapping("/api/v1/movies/{movieId}/rating")
    public ResponseEntity<RatingResponseDto> updateRating(
            @PathVariable
            @Positive(message = "movieId must be greater than 0")
            Long movieId,

            @Valid @RequestBody RatingRequestDto requestDto,
            JwtAuthenticationToken authentication
    )
    {
        Long currentUserId = authentication.getToken().getClaim("userId");

        return ResponseEntity.ok(ratingService.updateRating(movieId, currentUserId, requestDto));
    }

    @DeleteMapping("/api/v1/movies/{movieId}/rating")
    public ResponseEntity<Void> deleteRating(
            @PathVariable
            @Positive(message = "movieId must be greater than 0")
            Long movieId,
            JwtAuthenticationToken authentication
    )
    {
        Long currentUserId = authentication.getToken().getClaim("userId");

        ratingService.deleteRating(movieId, currentUserId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/users/me/ratings")
    public ResponseEntity<List<UserRatingResponseDto>> getMyRatings(JwtAuthenticationToken authentication)
    {
        Long currentUserId = authentication.getToken().getClaim("userId");

        return ResponseEntity.ok(ratingService.getMyRatings(currentUserId));
    }
}