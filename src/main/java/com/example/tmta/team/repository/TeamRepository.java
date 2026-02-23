package com.example.tmta.team.repository;

import com.example.tmta.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, java.util.UUID> {
    Optional<Team> findByName(String name);
}
