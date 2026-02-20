package com.example.tmta.service;

import com.example.tmta.dto.team.DetailTeamList;
import com.example.tmta.dto.team.TeamListResponseDto;
import com.example.tmta.dto.team.TeamProfileSetupRequestDto;
import com.example.tmta.dto.team.TeamSaveRequestDto;
import com.example.tmta.dto.team.TeamSaveResponseDto;
import com.example.tmta.entity.Appointment;
import com.example.tmta.entity.Member;
import com.example.tmta.entity.Team;
import com.example.tmta.entity.TeamMembers;
import com.example.tmta.entity.type.NamePolicy;
import com.example.tmta.entity.type.PostPermission;
import com.example.tmta.entity.type.TeamRole;
import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import com.example.tmta.repository.AppointmentRepository;
import com.example.tmta.repository.MemberRepository;
import com.example.tmta.repository.TeamMembersRepository;
import com.example.tmta.repository.TeamRepository;
import com.example.tmta.security.CurrentMemberProvider;
import com.example.tmta.service.assembler.TeamQueryAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final TeamMembersRepository teamMembersRepository;
    private final AppointmentRepository appointmentRepository;
    private final CurrentMemberProvider currentMemberProvider;
    private final TeamQueryAssembler teamQueryAssembler;

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

        return teamQueryAssembler.toTeamListResponse(myMemberships, teamMap, membersByTeam, appointmentsByTeam, memberMap);
    }

    @Transactional
    public TeamSaveResponseDto createTeam(TeamSaveRequestDto requestDto) {
        Member current = getCurrentMemberWithProfile();
        String normalizedTeamName = normalizeTeamName(requestDto.teamName());

        Team team = Team.create(
                normalizedTeamName,
                requestDto.useRealName() ? NamePolicy.USE_REALNAME : NamePolicy.USE_NICKNAME,
                requestDto.onlyLeaderCanPost() ? PostPermission.LEADER_ONLY : PostPermission.ALL_MEMBERS
        );
        teamRepository.save(team);

        TeamMembers creatorMembership = TeamMembers.createLeader(
                team.getId(), current.getId(), current.getNickName(), current.getProfileImage()
        );
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

        List<Appointment> appointments = appointmentRepository.findAllByTeamId(teamId);
        return teamQueryAssembler.toDetailTeamList(team, memberships, appointments, memberMap);
    }

    @Transactional
    public void joinTeam(UUID teamId) {
        Member current = getCurrentMemberWithProfile();
        getTeam(teamId);

        if (teamMembersRepository.existsByTeamIdAndMemberId(teamId, current.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED_TEAM);
        }

        TeamMembers membership = TeamMembers.createGeneral(teamId, current.getId());
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

    private String normalizeTeamName(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return teamName.trim();
    }
}
