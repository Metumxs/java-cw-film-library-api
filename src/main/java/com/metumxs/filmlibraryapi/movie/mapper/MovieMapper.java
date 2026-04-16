package com.metumxs.filmlibraryapi.movie.mapper;

import com.metumxs.filmlibraryapi.domain.entity.Genre;
import com.metumxs.filmlibraryapi.domain.entity.Movie;
import com.metumxs.filmlibraryapi.movie.dto.MovieDetailsResponseDto;
import com.metumxs.filmlibraryapi.movie.dto.MovieSummaryResponseDto;
import com.metumxs.filmlibraryapi.movie.dto.CreateMovieRequestDto;
import com.metumxs.filmlibraryapi.movie.dto.UpdateMovieRequestDto;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface MovieMapper
{
    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "title", source = "movie.title")
    @Mapping(target = "releaseYear", source = "movie.releaseYear")
    @Mapping(target = "genres", source = "movie.genres", qualifiedByName = "mapGenresToNames")
    @Mapping(target = "averageRating", source = "averageRating")
    @Mapping(target = "ratingsCount", source = "ratingsCount")
    MovieSummaryResponseDto toSummaryResponseDto(Movie movie, Double averageRating, Long ratingsCount);

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "title", source = "movie.title")
    @Mapping(target = "description", source = "movie.description")
    @Mapping(target = "releaseYear", source = "movie.releaseYear")
    @Mapping(target = "durationMinutes", source = "movie.durationMinutes")
    @Mapping(target = "country", source = "movie.country")
    @Mapping(target = "genres", source = "movie.genres", qualifiedByName = "mapGenresToNames")
    @Mapping(target = "averageRating", source = "averageRating")
    @Mapping(target = "ratingsCount", source = "ratingsCount")
    MovieDetailsResponseDto toDetailsResponseDto(Movie movie, Double averageRating, Long ratingsCount);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "genres", ignore = true)
    Movie toEntity(CreateMovieRequestDto requestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "genres", ignore = true)
    void updateEntityFromDto(UpdateMovieRequestDto requestDto, @MappingTarget Movie movie);

    @Named("mapGenresToNames")
    default Set<String> mapGenresToNames(Set<Genre> genres)
    {
        if (genres == null || genres.isEmpty())
        {
            return new LinkedHashSet<>();
        }

        return genres.stream()
                .map(Genre::getName)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}