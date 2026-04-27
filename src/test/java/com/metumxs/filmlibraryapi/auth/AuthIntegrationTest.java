package com.metumxs.filmlibraryapi.auth;

import com.metumxs.filmlibraryapi.AbstractBaseIntegrationTest;
import com.metumxs.filmlibraryapi.auth.dto.LoginRequestDto;
import com.metumxs.filmlibraryapi.auth.dto.RegistrationRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Блок тестів: Модуль автентифікації (TS-01..TS-07)")
class AuthIntegrationTest extends AbstractBaseIntegrationTest
{
    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Test
    @DisplayName("TS-01: Успішна реєстрація нового користувача")
    void testSuccessfulRegistration() throws Exception
    {
        RegistrationRequestDto request = new RegistrationRequestDto(
                "John Doe",
                "john.doe@example.com",
                "SecurePass123!"
        );

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("TS-02: Помилка реєстрації при дублюванні email")
    void testRegistrationWithExistingEmail() throws Exception
    {
        RegistrationRequestDto firstRequest = new RegistrationRequestDto(
                "First User",
                "duplicate@example.com",
                "SecurePass123!"
        );

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        RegistrationRequestDto secondRequest = new RegistrationRequestDto(
                "Second User",
                "duplicate@example.com",
                "AnotherPass123!"
        );

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("User with email duplicate@example.com already exists"));
    }

    @Test
    @DisplayName("TS-03: Помилка реєстрації при невалідних вхідних даних")
    void testRegistrationWithInvalidData() throws Exception
    {
        RegistrationRequestDto request = new RegistrationRequestDto(
                "JD", // Too short name
                "not-an-email", // Invalid email format
                "123" // Too short password
        );

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("password")));
    }

    @Test
    @DisplayName("TS-04: Успішний вхід у систему (отримання JWT)")
    void testSuccessfulLogin() throws Exception
    {
        RegistrationRequestDto regRequest = new RegistrationRequestDto(
                "Login Test",
                "login.test@example.com",
                "MyPassword2024!"
        );

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        LoginRequestDto loginRequest = new LoginRequestDto("login.test@example.com", "MyPassword2024!");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()));
    }

    @Test
    @DisplayName("TS-05: Помилка входу з невірним паролем")
    void testLoginWithWrongPassword() throws Exception
    {
        RegistrationRequestDto regRequest = new RegistrationRequestDto(
                "Wrong Pass User",
                "wrong.pass@example.com",
                "CorrectPassword123!"
        );

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        LoginRequestDto loginRequest = new LoginRequestDto("wrong.pass@example.com", "WrongPassword123!");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("TS-06: Помилка входу з неіснуючим email")
    void testLoginWithNonExistentEmail() throws Exception
    {
        LoginRequestDto loginRequest = new LoginRequestDto("nobody@example.com", "SomePassword123!");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("TS-07: Помилка входу при невалідних вхідних даних")
    void testLoginWithInvalidDataFormat() throws Exception
    {
        LoginRequestDto loginRequest = new LoginRequestDto("bad-email", "");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}