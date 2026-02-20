package com.example.tmta.service;

import com.example.tmta.dto.MemberInfo;
import com.example.tmta.dto.team.DetailTeamList;
import com.example.tmta.dto.team.TeamListResponseDto;
import com.example.tmta.dto.team.TeamProfileSetupRequestDto;
import com.example.tmta.dto.team.TeamSaveRequestDto;
import com.example.tmta.dto.team.TeamSaveResponseDto;
import com.example.tmta.entity.Appointment;
import com.example.tmta.entity.Member;
import com.example.tmta.entity.Team;
import com.example.tmta.entity.TeamMembers;
import com.example.tmta.entity.type.AppointmentState;
import com.example.tmta.entity.type.InviteState;
import com.example.tmta.entity.type.TeamRole;
import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import com.example.tmta.repository.AppointmentRepository;
import com.example.tmta.repository.MemberRepository;
import com.example.tmta.repository.TeamMembersRepository;
import com.example.tmta.repository.TeamRepository;
import com.example.tmta.security.CurrentMemberProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final TeamMembersRepository teamMembersRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentMemberProvider currentMemberProvider;

    @Transactional(readOnly = true)
    public TeamListResponseDto getTeamList() {
        Member current = getCurrentMemberWithProfile();
        List<TeamMembers> myMemberships = teamMembersRepository.findAllByMemberId(current.getId());
        if (myMemberships.isEmpty()) {
            return new TeamListResponseDto(List.of());
        }

        List<UUID> teamIds = myMemberships.stream().map(TeamMembers::getTeamId).distinct().toList();
        Map<UUID, Team> teamMap = teamRepository.findAllById(teamIds).stream()
                .collect(Collectors.toMap(Team::getId, team -> team));
        Map<UUID, List<TeamMembers>> membersByTeam = teamMembersRepository.findAllByTeamIdIn(teamIds).stream()
                .collect(Collectors.groupingBy(TeamMembers::getTeamId));
        Map<UUID, List<Appointment>> appointmentsByTeam = appointmentRepository.findAllByTeamIdIn(teamIds).stream()
                .collect(Collectors.groupingBy(Appointment::getTeamId));

        List<Long> memberIds = membersByTeam.values().stream()
                .flatMap(List::stream)
                .map(TeamMembers::getMemberId)
                .distinct()
                .toList();
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member));

        List<TeamListResponseDto.TeamInfo> teamInfoList = new ArrayList<>();
        for (TeamMembers myMembership : myMemberships) {
            Team team = teamMap.get(myMembership.getTeamId());
            if (team == null) {
                continue;
            }

            List<TeamMembers> teamMembers = membersByTeam.getOrDefault(team.getId(), List.of());
            List<Appointment> teamAppointments = appointmentsByTeam.getOrDefault(team.getId(), List.of());

            List<String> profiles = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (TeamMembers membership : teamMembers) {
                Member member = memberMap.get(membership.getMemberId());
                if (member == null) {
                    continue;
                }
                profiles.add(membership.getTeamProfileImage() != null ? membership.getTeamProfileImage() : member.getProfileImage());

                String displayName = membership.getTeamNickName() != null ? membership.getTeamNickName() : member.getNickName();
                if (displayName == null) {
                    displayName = member.getName();
                }
                names.add(displayName);
            }
            teamInfoList.add(new TeamListResponseDto.TeamInfo(
                    team.getId(),
                    team.getName(),
                    teamAppointments.isEmpty() ? AppointmentState.CREATING : teamAppointments.get(0).getState(),
                    (long) teamMembers.size(),
                    profiles,
                    names,
                    myMembership.isTeamProfileSetupCompleted()
            ));
        }

        return new TeamListResponseDto(teamInfoList);
    }

    @Transactional
    public TeamSaveResponseDto createTeam(TeamSaveRequestDto requestDto) {
        Member current = getCurrentMemberWithProfile();
        String normalizedTeamName = normalizeTeamName(requestDto.teamName());

        Team team = requestDto.toEntity(normalizedTeamName);
        teamRepository.save(team);

        TeamMembers creatorMembership = TeamMembers.builder()
                .teamId(team.getId())
                .memberId(current.getId())
                .inviteState(InviteState.ACCEPT)
                .teamRole(TeamRole.ADMIN)
                .teamNickName(current.getNickName())
                .teamProfileImage(current.getProfileImage())
                .teamProfileSetupCompleted(true)
                .build();
        teamMembersRepository.save(creatorMembership);

        return new TeamSaveResponseDto(team);
    }

    @Transactional(readOnly = true)
    public DetailTeamList getDetailTeam(UUID teamId) {
        Member current = getCurrentMemberWithProfile();
        Team team = getTeam(teamId);
        requireMembership(teamId, current.getId());

        List<TeamMembers> memberships = teamMembersRepository.findAllByTeamId(teamId);
        List<Long> memberIds = memberships.stream().map(TeamMembers::getMemberId).distinct().toList();
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member));

        List<MemberInfo> members = memberships.stream()
                .map(membership -> toMemberInfo(membership, memberMap.get(membership.getMemberId())))
                .toList();

        List<Appointment> appointments = appointmentRepository.findAllByTeamId(teamId);
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

    @Transactional
    public void joinTeam(UUID teamId) {
        Member current = getCurrentMemberWithProfile();
        getTeam(teamId);

        if (teamMembersRepository.existsByTeamIdAndMemberId(teamId, current.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED_TEAM);
        }

        TeamMembers membership = TeamMembers.builder()
                .teamId(teamId)
                .memberId(current.getId())
                .inviteState(InviteState.PENDING)
                .teamRole(TeamRole.GENERAL)
                .teamNickName(null)
                .teamProfileImage(null)
                .teamProfileSetupCompleted(false)
                .build();
        teamMembersRepository.save(membership);
    }

    @Transactional
    public void setupMyTeamProfile(UUID teamId, TeamProfileSetupRequestDto request) {
        Member current = getCurrentMemberWithProfile();
        getTeam(teamId);

        TeamMembers membership = requireMembership(teamId, current.getId());
        membership.updateTeamProfile(request.teamNickName(), request.teamProfileImage());
    }

    @Transactional
    public void exitTeam(UUID teamId) {
        Member current = getCurrentMemberWithProfile();
        getTeam(teamId);

        TeamMembers membership = requireMembership(teamId, current.getId());
        if (membership.getTeamRole() == TeamRole.ADMIN) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_EXIT_TEAM);
        }

        teamMembersRepository.delete(membership);
    }

    @Transactional
    public void delegateLeader(UUID teamId, Long memberId) {
        Member currentLeader = getCurrentMemberWithProfile();
        getTeam(teamId);

        TeamMembers leaderMembership = requireMembership(teamId, currentLeader.getId());
        requireLeader(leaderMembership);

        memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        TeamMembers newLeaderMembership = requireMembership(teamId, memberId);
        leaderMembership.updateTeamRole(TeamRole.GENERAL);
        newLeaderMembership.updateTeamRole(TeamRole.ADMIN);
    }

    @Transactional
    public void kickMember(UUID teamId, Long memberId) {
        Member currentLeader = getCurrentMemberWithProfile();
        getTeam(teamId);

        TeamMembers leaderMembership = requireMembership(teamId, currentLeader.getId());
        requireLeader(leaderMembership);
        if (currentLeader.getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_LEADER);
        }

        memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        TeamMembers targetMembership = requireMembership(teamId, memberId);
        teamMembersRepository.delete(targetMembership);
    }

    private Member getCurrentMemberWithProfile() {
        Member current = currentMemberProvider.getCurrentMember();
        if (!current.isProfileSetupCompleted()) {
            throw new BusinessException(ErrorCode.PROFILE_SETUP_REQUIRED);
        }
        return current;
    }

    private Team getTeam(UUID teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private TeamMembers requireMembership(UUID teamId, Long memberId) {
        return teamMembersRepository.findByTeamIdAndMemberId(teamId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM));
    }

    private void requireLeader(TeamMembers membership) {
        if (membership.getTeamRole() != TeamRole.ADMIN) {
            throw new BusinessException(ErrorCode.NOT_A_LEADER_OF_TEAM);
        }
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
                .map(date -> date.getDate())
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

    private String normalizeTeamName(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return teamName.trim();
    }
}
