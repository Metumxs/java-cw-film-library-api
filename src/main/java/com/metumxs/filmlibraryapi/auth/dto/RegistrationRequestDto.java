package com.metumxs.filmlibraryapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import static com.metumxs.filmlibraryapi.validation.ValidationConstants.*;

public record RegistrationRequestDto(
        @NotBlank(message = "name {validation.notBlank}")
        @Size(max = USER_NAME_MAX_LENGTH, message = "{user.name.maxSize}")
        String name,

        @NotBlank(message = "email {validation.notBlank}")
        @Email(message = "{user.email.format}")
        @Size(max = USER_EMAIL_MAX_LENGTH, message = "{user.email.maxSize}")
        String email,

        @NotBlank(message = "password {validation.notBlank}")
        @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH, message = "{user.password.sizeRange}")
        String password
)
{
}