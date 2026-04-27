package com.metumxs.filmlibraryapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metumxs.filmlibraryapi.auth.dto.LoginRequestDto;
import com.metumxs.filmlibraryapi.auth.dto.RegistrationRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
@ActiveProfiles("test")
public abstract class AbstractBaseIntegrationTest
{
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // -- HELPER METHODS --

    // Method to quickly create user and obtain its JWT token
    protected String obtainAccessToken(String name, String email, String password) throws Exception
    {
        RegistrationRequestDto regRequest = new RegistrationRequestDto(name, email, password);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/register")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regRequest)));

        LoginRequestDto loginRequest = new LoginRequestDto(email, password);
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn().getResponse().getContentAsString();

        return "Bearer " + objectMapper.readTree(response).get("accessToken").asText();
    }
}