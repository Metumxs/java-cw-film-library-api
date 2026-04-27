package com.metumxs.filmlibraryapi.movie;

import com.fasterxml.jackson.databind.JsonNode;
import com.metumxs.filmlibraryapi.AbstractBaseIntegrationTest;
import com.metumxs.filmlibraryapi.movie.dto.CreateMovieRequestDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Блок тестів: Модуль каталогу фільмів (TS-08..TS-13)")
class MovieCatalogIntegrationTest extends AbstractBaseIntegrationTest
{
    private static final String MOVIES_URL = "/api/v1/movies";
    private static final String ADMIN_MOVIES_URL = "/api/v1/admin/movies";

    @Autowired
    private JwtTokenService jwtTokenService;

    private Long uniqueTestMovieId;
    private String uniqueTestTitle;
    private static final int VALID_TEST_YEAR = 2020;

    @BeforeEach
    void setUp() throws Exception
    {
        CustomUserDetails admin = new CustomUserDetails(1L, "admin@mail.com", "pass", "ADMIN");
        String adminToken = "Bearer " + jwtTokenService.generateAccessToken(admin);

        uniqueTestTitle = "Test Movie " + System.currentTimeMillis();

        CreateMovieRequestDto request = new CreateMovieRequestDto(
                uniqueTestTitle,
                "Description",
                VALID_TEST_YEAR,
                120,
                "USA",
                Set.of(1L) // Genre 'Action'
        );

        String responseContent = mockMvc.perform(post(ADMIN_MOVIES_URL)
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(responseContent);
        uniqueTestMovieId = jsonNode.get("id").asLong();
    }

    @Test
    @DisplayName("TS-08: Успішне отримання списку фільмів")
    void testGetAllMovies() throws Exception
    {
        mockMvc.perform(get(MOVIES_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("TS-09: Успішний пошук фільму за назвою")
    void testSearchMoviesByTitle() throws Exception
    {
        // Search specifically for unique movie created in setUp()
        mockMvc.perform(get(MOVIES_URL)
                        .param("title", uniqueTestTitle)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value(uniqueTestTitle));
    }

    @Test
    @DisplayName("TS-10: Успішна фільтрація фільмів за жанром і/або роком")
    void testFilterMoviesByGenreAndYear() throws Exception
    {
        mockMvc.perform(get(MOVIES_URL)
                        .param("genre", "Action") // Genre 1L
                        .param("releaseYear", String.valueOf(VALID_TEST_YEAR))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].title", hasItem(uniqueTestTitle)))
                .andExpect(jsonPath("$.content[0].releaseYear").value(VALID_TEST_YEAR));
    }

    @Test
    @DisplayName("TS-11: Помилка отримання каталогу при некоректних параметрах запиту")
    void testGetMoviesWithInvalidParams() throws Exception
    {
        mockMvc.perform(get(MOVIES_URL)
                        .param("releaseYear", "invalid_year_format")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("TS-12: Успішне отримання деталей наявного фільму")
    void testGetMovieDetailsSuccess() throws Exception
    {
        // Fetch details of dynamically created in setUp() movie
        mockMvc.perform(get(MOVIES_URL + "/{id}", uniqueTestMovieId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uniqueTestMovieId))
                .andExpect(jsonPath("$.title").value(uniqueTestTitle))
                .andExpect(jsonPath("$.releaseYear").value(VALID_TEST_YEAR))
                .andExpect(jsonPath("$.durationMinutes").value(120))
                .andExpect(jsonPath("$.country").value("USA"))
                .andExpect(jsonPath("$.genres").isArray());
    }

    @Test
    @DisplayName("TS-13: Помилка при запиті деталей неіснуючого фільму")
    void testGetMovieDetailsNotFound() throws Exception
    {
        Long nonExistentId = 99999999L;

        mockMvc.perform(get(MOVIES_URL + "/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Movie with id " + nonExistentId + " not found"));
    }
}