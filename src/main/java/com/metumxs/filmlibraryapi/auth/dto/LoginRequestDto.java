package com.metumxs.filmlibraryapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import static com.metumxs.filmlibraryapi.validation.ValidationConstants.*;

public record LoginRequestDto(
        @NotBlank(message = "email {validation.notBlank}")
        @Email(message = "email {user.email.format}")
        @Size(max = USER_EMAIL_MAX_LENGTH, message = "email {user.email.size}")
        String email,

        @NotBlank(message = "password must not be blank")
        @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH, message = "{user.password.size}")
        String password
)
{
}