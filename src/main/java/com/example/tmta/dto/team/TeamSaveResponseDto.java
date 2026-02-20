package com.example.tmta.dto.team;

import com.example.tmta.entity.Team;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "팀 생성 응답 DTO")
public record TeamSaveResponseDto(
        @Schema(description = "팀 ID")
        UUID groupId
) {
    public TeamSaveResponseDto(Team team) {
        this(team.getId());
    }
}
