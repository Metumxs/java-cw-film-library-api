package com.metumxs.filmlibraryapi.genre.controller;

import com.metumxs.filmlibraryapi.domain.repository.GenreRepository;
import com.metumxs.filmlibraryapi.genre.dto.GenreDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
@Tag(name = "Genres", description = "Genres catalog endpoints")
public class GenreController
{
    private final GenreRepository genreRepository;

    @GetMapping
    @Operation(summary = "Provides a list of genres for dropdowns and filters")
    public List<GenreDto> getAllGenres()
    {
        return genreRepository.findAll().stream()
                .map(genre -> new GenreDto(genre.getId(), genre.getName()))
                .toList();
    }
}