package com.example.tmta.dto.team;

import com.example.tmta.entity.Team;
import com.example.tmta.entity.TeamMembers;
import com.example.tmta.entity.type.AppointmentState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Schema(description = "팀 목록 응답 DTO")
public class TeamListResponseDto {
    @Schema(description = "팀 목록")
    private List<TeamInfo> teamList;

    public TeamListResponseDto(List<TeamMembers> teamMembers) {
        this.teamList = teamMembers.stream()
                .map(tm -> new TeamInfo(tm.getTeam()))
                .collect(Collectors.toList());
    }

    @Getter
    @Schema(description = "팀 정보")
    public static class TeamInfo {
        @Schema(description = "팀 ID")
        private UUID groupId;
        @Schema(description = "팀 이름")
        private String groupName;
        @Schema(description = "약속 상태")
        private AppointmentState state;
        @Schema(description = "팀원 수")
        private Long memberCount;
        @Schema(description = "팀원 프로필 이미지 목록")
        private List<String> memberProfiles;

        public TeamInfo(Team team) {
            this.groupId = team.getId();
            this.groupName = team.getName();
            this.state = team.getAppointmentList().isEmpty() ? AppointmentState.CREATING : team.getAppointmentList().get(0).getState();
            this.memberCount = (long) team.getTeamMembersList().size();
            this.memberProfiles = team.getTeamMembersList().stream()
                    .map(tm -> tm.getMember().getNickName())
                    .collect(Collectors.toList());
        }
    }
}
