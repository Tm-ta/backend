package com.example.tmta.service.assembler;

import com.example.tmta.dto.MemberInfo;
import com.example.tmta.dto.team.DetailTeamList;
import com.example.tmta.dto.team.TeamListResponseDto;
import com.example.tmta.entity.Appointment;
import com.example.tmta.entity.AppointmentDate;
import com.example.tmta.entity.Member;
import com.example.tmta.entity.Team;
import com.example.tmta.entity.TeamMembers;
import com.example.tmta.entity.type.AppointmentState;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TeamQueryAssembler {

    public TeamListResponseDto toTeamListResponse(List<TeamMembers> myMemberships,
                                                  Map<UUID, Team> teamMap,
                                                  Map<UUID, List<TeamMembers>> membersByTeam,
                                                  Map<UUID, List<Appointment>> appointmentsByTeam,
                                                  Map<Long, Member> memberMap) {
        List<TeamListResponseDto.TeamInfo> teamInfoList = new ArrayList<>();
        for (TeamMembers myMembership : myMemberships) {
            Team team = teamMap.get(myMembership.getTeamId());
            if (team == null) {
                continue;
            }
            List<TeamMembers> teamMembers = membersByTeam.getOrDefault(team.getId(), List.of());
            List<Appointment> teamAppointments = appointmentsByTeam.getOrDefault(team.getId(), List.of());
            List<MemberInfo> members = teamMembers.stream()
                    .map(membership -> toMemberInfo(membership, memberMap.get(membership.getMemberId())))
                    .toList();
            teamInfoList.add(new TeamListResponseDto.TeamInfo(
                    team.getId(),
                    team.getName(),
                    teamAppointments.isEmpty() ? AppointmentState.CREATING : teamAppointments.get(0).getState(),
                    (long) teamMembers.size(),
                    members,
                    myMembership.isTeamProfileSetupCompleted()
            ));
        }
        return new TeamListResponseDto(teamInfoList);
    }

    public DetailTeamList toDetailTeamList(Team team,
                                           List<TeamMembers> memberships,
                                           List<Appointment> appointments,
                                           Map<Long, Member> memberMap) {
        List<MemberInfo> members = memberships.stream()
                .map(membership -> toMemberInfo(membership, memberMap.get(membership.getMemberId())))
                .toList();
        List<DetailTeamList.AppointmentDetail> appointmentDetails = appointments.stream()
                .map(appointment -> toAppointmentDetail(appointment, memberships.size()))
                .toList();

        return new DetailTeamList(
                team.getId(),
                team.getName(),
                (long) memberships.size(),
                team.getProfileImage(),
                appointmentDetails,
                members
        );
    }

    private MemberInfo toMemberInfo(TeamMembers membership, Member member) {
        if (member == null) {
            return MemberInfo.of(membership.getMemberId(), null, null);
        }
        String displayName = membership.getTeamNickName() != null ? membership.getTeamNickName() : member.getNickName();
        if (displayName == null) {
            displayName = member.getName();
        }
        String profileImage = membership.getTeamProfileImage() != null ? membership.getTeamProfileImage() : member.getProfileImage();
        return MemberInfo.of(member.getId(), displayName, profileImage);
    }

    private DetailTeamList.AppointmentDetail toAppointmentDetail(Appointment appointment, int memberCount) {
        List<LocalDate> dates = appointment.getAppointmentDateList().stream()
                .map(AppointmentDate::getDate)
                .sorted(Comparator.naturalOrder())
                .toList();
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (!dates.isEmpty()) {
            startDate = dates.get(0);
            endDate = dates.get(dates.size() - 1);
        }
        return new DetailTeamList.AppointmentDetail(
                appointment.getId(),
                startDate,
                endDate,
                (long) memberCount,
                appointment.getState(),
                appointment.isOnlyDate()
        );
    }
}
