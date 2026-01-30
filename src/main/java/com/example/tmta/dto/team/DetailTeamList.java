package com.example.tmta.dto.team;

import com.example.tmta.entity.Team;
import lombok.Getter;

import java.util.UUID;

@Getter
public class DetailTeamList {
    private UUID id;
    private String name;
    private String profileImage;

    public DetailTeamList(Team team) {
        this.id = team.getId();
        this.name = team.getName();
        this.profileImage = team.getProfileImage();
    }
}