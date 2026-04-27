package com.metumxs.filmlibraryapi.movie;

import com.fasterxml.jackson.databind.JsonNode;
import com.metumxs.filmlibraryapi.AbstractBaseIntegrationTest;
import com.metumxs.filmlibraryapi.movie.dto.CreateMovieRequestDto;
import com.metumxs.filmlibraryapi.movie.dto.UpdateMovieRequestDto;
import com.metumxs.filmlibraryapi.security.CustomUserDetails;
import com.metumxs.filmlibraryapi.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Блок тестів: Адміністративний модуль (TS-28..TS-35)")
class AdminMovieIntegrationTest extends AbstractBaseIntegrationTest
{
    @Autowired
    private JwtTokenService jwtTokenService;

    private static final String ADMIN_MOVIES_BASE_URL = "/api/v1/admin/movies";
    private static final String ADMIN_MOVIES_ID_URL = ADMIN_MOVIES_BASE_URL + "/{id}";
    private static final Long NON_EXISTENT_MOVIE_ID = 999999L;

    private String adminToken;
    private String userToken;

    // ID of the movie created dynamically for update/delete tests
    private Long testMovieId;

    @BeforeEach
    void setUp() throws Exception
    {
        CustomUserDetails adminMockUser = new CustomUserDetails(
                1L,
                "admin@example.com",
                "dummy_admin_pass",
                "ADMIN"
        );
        adminToken = "Bearer " + jwtTokenService.generateAccessToken(adminMockUser);

        CustomUserDetails regularMockUser = new CustomUserDetails(
                2L,
                "user@example.com",
                "dummy_user_pass",
                "USER"
        );
        userToken = "Bearer " + jwtTokenService.generateAccessToken(regularMockUser);

        // Create a test movie to safely use in PUT/DELETE tests
        CreateMovieRequestDto setupRequest = new CreateMovieRequestDto(
                "Initial Test Movie",
                "Created dynamically in BeforeEach",
                2020,
                100,
                "USA",
                Set.of(1L)
        );

        String responseContent = mockMvc.perform(post(ADMIN_MOVIES_BASE_URL)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(setupRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(responseContent);
        testMovieId = jsonNode.get("id").asLong();
    }

    @Test
    @DisplayName("TS-28: Успішне створення фільму адміністратором")
    void testCreateMovieByAdminSuccess() throws Exception
    {
        CreateMovieRequestDto request = new CreateMovieRequestDto(
                "New Awesome Movie",
                "Description for the new movie",
                2024,
                120,
                "USA",
                Set.of(1L, 2L)
        );

        mockMvc.perform(post(ADMIN_MOVIES_BASE_URL)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.title").value("New Awesome Movie"))
                .andExpect(jsonPath("$.genres", hasSize(2)));
    }

    @Test
    @DisplayName("TS-29: Помилка створення фільму адміністратором при невалідних даних")
    void testCreateMovieByAdminInvalidData() throws Exception
    {
        CreateMovieRequestDto request = new CreateMovieRequestDto(
                "Invalid Movie",
                "Description",
                2024,
                120,
                "USA",
                Set.of() // Invalid: must not be empty
        );

        mockMvc.perform(post(ADMIN_MOVIES_BASE_URL)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("TS-30: Помилка створення фільму звичайним користувачем")
    void testCreateMovieByUserForbidden() throws Exception
    {
        CreateMovieRequestDto request = new CreateMovieRequestDto(
                "User Movie",
                "Users should not be able to create movies",
                2024,
                120,
                "USA",
                Set.of(1L)
        );

        mockMvc.perform(post(ADMIN_MOVIES_BASE_URL)
                        .header(HttpHeaders.AUTHORIZATION, userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("TS-31: Успішне оновлення фільму адміністратором")
    void testUpdateMovieByAdminSuccess() throws Exception
    {
        UpdateMovieRequestDto request = new UpdateMovieRequestDto(
                "Inception: Director's Cut",
                "Updated description",
                2010,
                150,
                "USA",
                Set.of(1L, 2L)
        );

        // Using dynamically created testMovieId instead of hardcoded EXISTING_MOVIE_ID
        mockMvc.perform(put(ADMIN_MOVIES_ID_URL, testMovieId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testMovieId))
                .andExpect(jsonPath("$.title").value("Inception: Director's Cut"))
                .andExpect(jsonPath("$.durationMinutes").value(150));
    }

    @Test
    @DisplayName("TS-32: Помилка оновлення фільму адміністратором при невалідних даних")
    void testUpdateMovieByAdminInvalidData() throws Exception
    {
        UpdateMovieRequestDto request = new UpdateMovieRequestDto(
                "Bad Year Movie",
                "Desc",
                1000, // Invalid year
                150,
                "USA",
                Set.of(1L)
        );

        mockMvc.perform(put(ADMIN_MOVIES_ID_URL, testMovieId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("TS-33: Помилка оновлення неіснуючого фільму адміністратором")
    void testUpdateNonExistentMovieByAdmin() throws Exception
    {
        UpdateMovieRequestDto request = new UpdateMovieRequestDto(
                "Ghost Movie",
                "Does not exist",
                2020,
                100,
                "USA",
                Set.of(1L)
        );

        mockMvc.perform(put(ADMIN_MOVIES_ID_URL, NON_EXISTENT_MOVIE_ID)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Movie with id " + NON_EXISTENT_MOVIE_ID + " not found"));
    }

    @Test
    @DisplayName("TS-34: Успішне видалення фільму адміністратором")
    void testDeleteMovieByAdminSuccess() throws Exception
    {
        // Deleting the movie created in @BeforeEach
        mockMvc.perform(delete(ADMIN_MOVIES_ID_URL, testMovieId)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNoContent());

        // Verify it was actually deleted
        mockMvc.perform(get("/api/v1/movies/{id}", testMovieId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TS-35: Помилка видалення неіснуючого фільму адміністратором")
    void testDeleteNonExistentMovieByAdmin() throws Exception
    {
        mockMvc.perform(delete(ADMIN_MOVIES_ID_URL, NON_EXISTENT_MOVIE_ID)
                        .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Movie with id " + NON_EXISTENT_MOVIE_ID + " not found"));
    }
}