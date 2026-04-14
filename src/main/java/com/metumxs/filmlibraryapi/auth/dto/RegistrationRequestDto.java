package com.metumxs.filmlibraryapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequestDto(
        @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must be less than or equal to 100 characters")
        String name,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a well-formed email address")
        @Size(max = 255, message = "email must be less than or equal to 255 characters")
        String email,

        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 255, message = "password size must be between 8 and 255 characters")
        String password
)
{
}