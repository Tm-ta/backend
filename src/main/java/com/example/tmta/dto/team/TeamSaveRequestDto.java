package com.example.tmta.dto.team;

import com.example.tmta.entity.Team;
import com.example.tmta.entity.type.NamePolicy;
import com.example.tmta.entity.type.PostPermission;
import lombok.Getter;

@Getter
public class TeamSaveRequestDto {
    private String teamName;
    private Boolean useRealName;
    private Boolean onlyLeaderCanPost;

    public Team toEntity() {
        return Team.builder()
                .name(teamName)
                .namePolicy(useRealName ? NamePolicy.USE_REAL_NAME : NamePolicy.USE_NICKNAME)
                .postPermission(onlyLeaderCanPost ? PostPermission.LEADER_ONLY : PostPermission.MEMBER_ALL)
                .build();
    }
}
