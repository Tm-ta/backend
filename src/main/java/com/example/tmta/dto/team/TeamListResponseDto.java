package com.example.tmta.dto.team;

import com.example.tmta.entity.type.AppointmentState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "팀 목록 응답 DTO")
public class TeamListResponseDto {
    @Schema(description = "팀 목록")
    private List<TeamInfo> teamList;

    @Data
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
        @Schema(description = "팀에서 사용하는 팀원 이름 목록")
        private List<String> memberNames;
        @Schema(description = "내 팀 프로필 설정 완료 여부")
        private boolean myTeamProfileSetupCompleted;
    }
}
