package com.example.tmta.dto.team;

import com.example.tmta.dto.MemberInfo;
import com.example.tmta.entity.type.AppointmentState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "팀 상세 정보 응답 DTO")
public class DetailTeamList {
    @Schema(description = "팀 ID")
    private UUID groupId;
    @Schema(description = "팀 이름")
    private String groupName;
    @Schema(description = "팀원 수")
    private Long memberCount;
    @Schema(description = "팀 프로필 이미지")
    private String profileImage;
    @Schema(description = "약속 목록")
    private List<AppointmentDetail> appointments;
    @Schema(description = "팀 멤버 목록")
    private List<MemberInfo> members;

    @Data
    @Schema(description = "약속 상세 정보")
    public static class AppointmentDetail {
        @Schema(description = "약속 ID")
        private UUID appointmentId;
        @Schema(description = "시작일")
        private LocalDate startDate;
        @Schema(description = "종료일")
        private LocalDate endDate;
        @Schema(description = "참여 인원")
        private Long memberCount;
        @Schema(description = "약속 상태")
        private AppointmentState state;
        @Schema(description = "날짜만 선택 가능한 약속인지 여부")
        private boolean isOnlyDate;
    }
}
