package com.example.tmta.repository;

import com.example.tmta.entity.Member;
import com.example.tmta.entity.Team;
import com.example.tmta.entity.TeamMembers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamMembersRepository extends JpaRepository<TeamMembers, Long> {
    Optional<TeamMembers> findByTeamAndMember(Team team, Member member);
    java.util.List<TeamMembers> findAllByTeam(Team team);
    java.util.List<TeamMembers> findAllByMember(Member member);
}

