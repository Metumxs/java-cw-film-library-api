package com.metumxs.filmlibraryapi.rating;

import com.fasterxml.jackson.databind.JsonNode;
import com.metumxs.filmlibraryapi.AbstractBaseIntegrationTest;
import com.metumxs.filmlibraryapi.movie.dto.CreateMovieRequestDto;
import com.metumxs.filmlibraryapi.rating.dto.RatingRequestDto;
import com.metumxs.filmlibraryapi.security.CustomUserDetails;
import com.metumxs.filmlibraryapi.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Set;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Блок тестів: Модуль оцінювання фільмів (TS-14..TS-27)")
class RatingIntegrationTest extends AbstractBaseIntegrationTest
{
    @Autowired
    private JwtTokenService jwtTokenService;

    private static final String MOVIE_RATING_URL = "/api/v1/movies/{id}/rating";
    private static final String USER_RATINGS_URL = "/api/v1/users/me/ratings";
    private static final String ADMIN_MOVIES_URL = "/api/v1/admin/movies";
    private static final Long NON_EXISTENT_MOVIE_ID = 999999L;

    private String userToken;
    private String secondUserToken;

    // Dynamically generated movie IDs
    private Long movie1Id;
    private Long movie2Id;

    @BeforeEach
    void setUp() throws Exception
    {
        long timestamp = System.currentTimeMillis();
        userToken = obtainAccessToken("Rating User", "user1_" + timestamp + "@mail.com", "Password123!");
        secondUserToken = obtainAccessToken("Second User", "user2_" + timestamp + "@mail.com", "Password123!");

        CustomUserDetails admin = new CustomUserDetails(
                1L,
                "admin@mail.com",
                "dummy_admin_pass",
                "ADMIN"
        );
        String adminToken = "Bearer " + jwtTokenService.generateAccessToken(admin);

        movie1Id = createTestMovie(adminToken, "Test Movie 1");
        movie2Id = createTestMovie(adminToken, "Test Movie 2");
    }

    // HELPER METHOD
    private Long createTestMovie(String adminToken, String title) throws Exception
    {
        CreateMovieRequestDto request = new CreateMovieRequestDto(
                title, "Dynamically created for rating tests", 2023, 120, "USA", Set.of(1L)
        );

        String responseContent = mockMvc.perform(post(ADMIN_MOVIES_URL)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(responseContent);
        return jsonNode.get("id").asLong();
    }

    @Test
    @DisplayName("TS-14: Успішне створення оцінки для фільму")
    void testCreateRatingSuccess() throws Exception
    {
        RatingRequestDto request = new RatingRequestDto(9);

        mockMvc.perform(post(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.value").value(9))
                .andExpect(jsonPath("$.movieId").value(movie1Id))
                .andExpect(jsonPath("$.userId").value(notNullValue()));
    }

    @Test
    @DisplayName("TS-15: Помилка створення оцінки без авторизації")
    void testCreateRatingUnauthorized() throws Exception
    {
        RatingRequestDto request = new RatingRequestDto(9);

        mockMvc.perform(post(MOVIE_RATING_URL, movie1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("TS-16: Помилка при спробі створити дубль оцінки")
    void testCreateRatingConflict() throws Exception
    {
        RatingRequestDto request = new RatingRequestDto(8);

        // First try (SUCCESS)
        mockMvc.perform(post(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second try (FAIL)
        mockMvc.perform(post(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Rating for movie " + movie1Id + " already exists for current user"));
    }

    @Test
    @DisplayName("TS-17: Помилка створення оцінки для неіснуючого фільму")
    void testCreateRatingMovieNotFound() throws Exception
    {
        RatingRequestDto request = new RatingRequestDto(8);

        mockMvc.perform(post(MOVIE_RATING_URL, NON_EXISTENT_MOVIE_ID)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Movie with id " + NON_EXISTENT_MOVIE_ID + " not found"));
    }

    @Test
    @DisplayName("TS-18: Успішне оновлення власної оцінки")
    void testUpdateRatingSuccess() throws Exception
    {
        mockMvc.perform(post(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RatingRequestDto(8))))
                .andExpect(status().isCreated());

        RatingRequestDto updateRequest = new RatingRequestDto(10);
        mockMvc.perform(put(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(10))
                .andExpect(jsonPath("$.movieId").value(movie1Id));
    }

    @Test
    @DisplayName("TS-19: Помилка при спробі оновити неіснуючу власну оцінку")
    void testUpdateRatingNotFound() throws Exception
    {
        RatingRequestDto updateRequest = new RatingRequestDto(10);

        mockMvc.perform(put(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Rating for movie " + movie1Id + " by current user not found"));
    }

    @Test
    @DisplayName("TS-20: Помилка при спробі оновити чужу оцінку")
    void testUpdateSomeoneElseRating() throws Exception
    {
        // User 1 creates rating
        mockMvc.perform(post(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RatingRequestDto(8))))
                .andExpect(status().isCreated());

        // User 2 tries to update it
        RatingRequestDto updateRequest = new RatingRequestDto(10);
        mockMvc.perform(put(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Rating for movie " + movie1Id + " by current user not found"));
    }

    @Test
    @DisplayName("TS-21: Помилка оновлення оцінки для неіснуючого фільму")
    void testUpdateRatingMovieNotFound() throws Exception
    {
        RatingRequestDto updateRequest = new RatingRequestDto(10);

        mockMvc.perform(put(MOVIE_RATING_URL, NON_EXISTENT_MOVIE_ID)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Movie with id " + NON_EXISTENT_MOVIE_ID + " not found"));
    }

    @Test
    @DisplayName("TS-22: Успішне видалення власної оцінки")
    void testDeleteRatingSuccess() throws Exception
    {
        mockMvc.perform(post(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RatingRequestDto(7))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isNoContent()); // 204 No Content
    }

    @Test
    @DisplayName("TS-23: Помилка видалення неіснуючої власної оцінки")
    void testDeleteRatingNotFound() throws Exception
    {
        mockMvc.perform(delete(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Rating for movie " + movie1Id + " by current user not found"));
    }

    @Test
    @DisplayName("TS-24: Помилка при спробі видалити чужу оцінку")
    void testDeleteSomeoneElseRating() throws Exception
    {
        // User 1 creates a rating
        mockMvc.perform(post(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RatingRequestDto(7))))
                .andExpect(status().isCreated());

        // User 2 is trying to delete it (User 2's rating is not found)
        mockMvc.perform(delete(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, secondUserToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Rating for movie " + movie1Id + " by current user not found"));
    }

    @Test
    @DisplayName("TS-25: Помилка видалення оцінки для неіснуючого фільму")
    void testDeleteRatingMovieNotFound() throws Exception
    {
        mockMvc.perform(delete(MOVIE_RATING_URL, NON_EXISTENT_MOVIE_ID)
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Movie with id " + NON_EXISTENT_MOVIE_ID + " not found"));
    }

    @Test
    @DisplayName("TS-26: Успішне отримання списку власних оцінок")
    void testGetUserRatingsSuccess() throws Exception
    {
        // Create ratings for dynamically generated movies
        mockMvc.perform(post(MOVIE_RATING_URL, movie1Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RatingRequestDto(7))))
                .andExpect(status().isCreated());

        mockMvc.perform(post(MOVIE_RATING_URL, movie2Id)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RatingRequestDto(9))))
                .andExpect(status().isCreated());

        // Get the list and check its content
        mockMvc.perform(get(USER_RATINGS_URL)
                        .header(HttpHeaders.AUTHORIZATION, userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].movieId").exists())
                .andExpect(jsonPath("$[0].movieTitle").exists())
                .andExpect(jsonPath("$[0].value").exists());
    }

    @Test
    @DisplayName("TS-27: Помилка отримання списку власних оцінок без авторизації")
    void testGetUserRatingsUnauthorized() throws Exception
    {
        mockMvc.perform(get(USER_RATINGS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}