package com.metumxs.filmlibraryapi.auth.service;

import com.metumxs.filmlibraryapi.auth.dto.RegistrationRequestDto;
import com.metumxs.filmlibraryapi.auth.dto.RegistrationResponseDto;
import com.metumxs.filmlibraryapi.domain.entity.Role;
import com.metumxs.filmlibraryapi.domain.entity.User;
import com.metumxs.filmlibraryapi.domain.repository.RoleRepository;
import com.metumxs.filmlibraryapi.domain.repository.UserRepository;
import com.metumxs.filmlibraryapi.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService
{
    private static final String DEFAULT_USER_ROLE = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationResponseDto register(RegistrationRequestDto requestDto)
    {
        String normalizedName = normalizeName(requestDto.name());
        String normalizedEmail = normalizeEmail(requestDto.email());

        if (userRepository.existsByEmail(normalizedEmail))
        {
            throw new ConflictException("User with email " + normalizedEmail + " already exists");
        }

        Role userRole = roleRepository.findByName(DEFAULT_USER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default role USER not found"));

        User user = new User();
        user.setName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(requestDto.password()));
        user.setRole(userRole);

        User savedUser = userRepository.save(user);

        return new RegistrationResponseDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().getName(),
                savedUser.getCreatedAt()
        );
    }

    // HELPER METHODS

    private String normalizeName(String name)
    {
        return name.trim();
    }

    private String normalizeEmail(String email)
    {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}