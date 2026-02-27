package com.example.tmta.team.repository;

import com.example.tmta.team.entity.TeamMembers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMembersRepository extends JpaRepository<TeamMembers, Long> {
    Optional<TeamMembers> findByTeamIdAndMemberId(UUID teamId, Long memberId);
    boolean existsByTeamIdAndMemberId(UUID teamId, Long memberId);
    List<TeamMembers> findAllByTeamId(UUID teamId);
    List<TeamMembers> findAllByTeamIdIn(List<UUID> teamIds);
    List<TeamMembers> findAllByMemberId(Long memberId);
    void deleteAllByMemberId(Long memberId);
}
