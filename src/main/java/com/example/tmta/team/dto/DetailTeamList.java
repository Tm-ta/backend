package com.example.tmta.team.dto;

import com.example.tmta.common.dto.MemberInfo;
import com.example.tmta.appointment.entity.type.AppointmentState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "팀 상세 정보 응답 DTO")
public record DetailTeamList(
        @Schema(description = "팀 ID") UUID groupId,
        @Schema(description = "팀 이름") String groupName,
        @Schema(description = "팀원 수") Long memberCount,
        @Schema(description = "팀 프로필 이미지") String profileImage,
        @Schema(description = "약속 목록") List<AppointmentDetail> appointments,
        @Schema(description = "팀 멤버 목록") List<MemberInfo> members
) {
    @Schema(description = "약속 상세 정보")
    public record AppointmentDetail(
            @Schema(description = "약속 ID") UUID appointmentId,
            @Schema(description = "시작일") LocalDate startDate,
            @Schema(description = "종료일") LocalDate endDate,
            @Schema(description = "참여 인원") Long memberCount,
            @Schema(description = "약속 상태") AppointmentState state,
            @Schema(description = "날짜만 선택 가능한 약속인지 여부") boolean isOnlyDate
    ) {
    }
}
