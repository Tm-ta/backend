package com.example.tmta.team.dto;

import com.example.tmta.team.entity.Team;
import com.example.tmta.team.entity.type.NamePolicy;
import com.example.tmta.team.entity.type.PostPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
@Schema(description = "팀 생성 요청 DTO")
public record TeamSaveRequestDto(
        @Schema(description = "팀 이름", example = "팀 프로젝트")
        @NotBlank String teamName,
        @Schema(description = "실명 사용 여부", example = "true")
        @NotNull Boolean useRealName,
        @Schema(description = "리더만 포스트 작성 가능 여부", example = "false")
        @NotNull Boolean onlyLeaderCanPost
) {

    public Team toEntity() {
        return toEntity(this.teamName);
    }

    public Team toEntity(String normalizedTeamName) {
        return Team.builder()
                .name(normalizedTeamName)
                .namePolicy(useRealName ? NamePolicy.USE_REALNAME : NamePolicy.USE_NICKNAME)
                .postPermission(onlyLeaderCanPost ? PostPermission.LEADER_ONLY : PostPermission.ALL_MEMBERS)
                .build();
    }
}
