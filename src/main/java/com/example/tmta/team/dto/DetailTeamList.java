package com.example.tmta.team.dto;

import com.example.tmta.common.dto.MemberInfo;
import com.example.tmta.appointment.entity.type.AppointmentState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "팀 상세 정보 응답 DTO")
public record DetailTeamList(
        @Schema(description = "팀 ID", example = "550e8400-e29b-41d4-a716-446655440000") UUID groupId,
        @Schema(description = "팀 이름", example = "Team Alpha") String groupName,
        @Schema(description = "팀원 수", example = "4") Long memberCount,
        @Schema(description = "팀 프로필 이미지 상세 경로(bucket/key)", example = "tmta-prod-assets/team-profile/1/20260228/team-alpha.png") String profileImagePath,
        @Schema(description = "약속 목록") List<AppointmentDetail> appointments,
        @Schema(description = "팀 멤버 목록") List<MemberInfo> members
) {
    @Schema(description = "약속 상세 정보")
    public record AppointmentDetail(
            @Schema(description = "약속 ID", example = "660e8400-e29b-41d4-a716-446655440000") UUID appointmentId,
            @Schema(description = "시작일", example = "2026-03-01") LocalDate startDate,
            @Schema(description = "종료일", example = "2026-03-05") LocalDate endDate,
            @Schema(description = "참여 인원", example = "4") Long memberCount,
            @Schema(description = "약속 상태") AppointmentState state,
            @Schema(description = "날짜만 선택 가능한 약속인지 여부", example = "false") boolean isOnlyDate
    ) {
    }
}
