package com.example.tmta.dto.team;

import com.example.tmta.entity.Team;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TeamSaveResponseDto {
    private UUID id;

    public TeamSaveResponseDto(Team team) {
        this.id = team.getId();
    }
}
