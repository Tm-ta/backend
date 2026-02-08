package com.example.tmta.dto.team;

import com.example.tmta.entity.Team;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.UUID;

@Getter
@Schema(description = "팀 생성 응답 DTO")
public class TeamSaveResponseDto {
    @Schema(description = "팀 ID")
    private UUID groupId;

    public TeamSaveResponseDto(Team team) {
        this.groupId = team.getId();
    }
}
