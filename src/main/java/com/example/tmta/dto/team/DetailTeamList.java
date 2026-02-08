package com.example.tmta.dto.team;

import com.example.tmta.entity.Appointment;
import com.example.tmta.entity.Team;
import com.example.tmta.entity.type.AppointmentState;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
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

    public DetailTeamList(Team team) {
        this.groupId = team.getId();
        this.groupName = team.getName();
        this.memberCount = (long) team.getTeamMembersList().size();
        this.profileImage = team.getProfileImage();
        this.appointments = team.getAppointmentList().stream()
                .map(AppointmentDetail::new)
                .collect(Collectors.toList());
    }

    @Getter
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

        public AppointmentDetail(Appointment appointment) {
            this.appointmentId = appointment.getId();
            this.startDate = appointment.getAppointmentDateList().get(0).getDate();
            this.endDate = appointment.getAppointmentDateList().get(appointment.getAppointmentDateList().size() - 1).getDate();
            this.memberCount = (long) appointment.getTeam().getTeamMembersList().size();
            this.state = appointment.getState();
        }
    }
}
