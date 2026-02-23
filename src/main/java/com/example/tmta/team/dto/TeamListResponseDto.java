package com.example.tmta.team.dto;

import com.example.tmta.common.dto.MemberInfo;
import com.example.tmta.appointment.entity.type.AppointmentState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "팀 목록 응답 DTO")
public record TeamListResponseDto(
        @Schema(description = "팀 목록")
        List<TeamInfo> teamList
) {
    @Schema(description = "팀 정보")
    public record TeamInfo(
            @Schema(description = "팀 ID") UUID groupId,
            @Schema(description = "팀 이름") String groupName,
            @Schema(description = "약속 상태") AppointmentState state,
            @Schema(description = "팀원 수") Long memberCount,
            @Schema(description = "팀원 정보 목록")
            List<MemberInfo> members,
            @Schema(description = "내 팀 프로필 설정 완료 여부") boolean myTeamProfileSetupCompleted
    ) {
    }
}
