package com.metumxs.filmlibraryapi.domain.repository;

import com.metumxs.filmlibraryapi.domain.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Long>
{
    boolean existsByUser_IdAndMovie_Id(Long userId, Long movieId);

    Optional<Rating> findByUser_IdAndMovie_Id(Long userId, Long movieId);

    List<Rating> findAllByUser_Id(Long userId);
}