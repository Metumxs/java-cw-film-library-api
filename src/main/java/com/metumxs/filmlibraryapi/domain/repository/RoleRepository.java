package com.metumxs.filmlibraryapi.domain.repository;

import com.metumxs.filmlibraryapi.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long>
{
    Optional<Role> findByName(String name);
}