package com.metumxs.filmlibraryapi.rating.controller;

import com.metumxs.filmlibraryapi.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.metumxs.filmlibraryapi.rating.dto.RatingRequestDto;
import com.metumxs.filmlibraryapi.rating.dto.RatingResponseDto;
import com.metumxs.filmlibraryapi.rating.dto.UserRatingResponseDto;
import com.metumxs.filmlibraryapi.rating.RatingService;
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
@Tag(name = "Ratings", description = "Endpoints for managing current user's movie ratings")
@SecurityRequirement(name = "bearerAuth")
public class RatingController
{
    private final RatingService ratingService;

    @Operation(summary = "Create rating for a movie")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rating created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Movie or user not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Rating already exists", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(summary = "Update current user's rating for a movie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Movie or rating not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(summary = "Delete current user's rating for a movie")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rating deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Movie or rating not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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

    @Operation(summary = "Get current user's ratings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ratings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/api/v1/users/me/ratings")
    public ResponseEntity<List<UserRatingResponseDto>> getMyRatings(JwtAuthenticationToken authentication)
    {
        Long currentUserId = authentication.getToken().getClaim("userId");

        return ResponseEntity.ok(ratingService.getUserRatings(currentUserId));
    }
}