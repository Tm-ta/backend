package com.example.tmta.dto.team;

import com.example.tmta.entity.Team;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class TeamListResponseDto {
    private List<DetailTeamList> teams;

    public TeamListResponseDto(List<Team> teams) {
        this.teams = teams.stream()
                .map(DetailTeamList::new)
                .collect(Collectors.toList());
    }
}