package com.example.tmta.repository;

import com.example.tmta.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, java.util.UUID> {
    Optional<Team> findByName(String name);
}
