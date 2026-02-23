package com.example.tmta.team;

import com.example.tmta.team.dto.DetailTeamList;
import com.example.tmta.team.dto.TeamListResponseDto;
import com.example.tmta.team.dto.TeamProfileSetupRequestDto;
import com.example.tmta.team.dto.TeamSaveRequestDto;
import com.example.tmta.team.dto.TeamSaveResponseDto;
import com.example.tmta.appointment.entity.Appointment;
import com.example.tmta.member.entity.Member;
import com.example.tmta.team.entity.Team;
import com.example.tmta.team.entity.TeamMembers;
import com.example.tmta.team.entity.type.NamePolicy;
import com.example.tmta.team.entity.type.PostPermission;
import com.example.tmta.team.entity.type.TeamRole;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.example.tmta.appointment.repository.AppointmentRepository;
import com.example.tmta.member.repository.MemberRepository;
import com.example.tmta.team.repository.TeamMembersRepository;
import com.example.tmta.team.repository.TeamRepository;
import com.example.tmta.common.security.CurrentMemberProvider;
import com.example.tmta.team.TeamQueryAssembler;
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

    /** 현재 사용자가 속한 팀 목록을 조회합니다. */
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

    /** 새 팀을 생성하고 생성자를 팀장으로 등록합니다. */
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

    /** 팀 상세 정보(멤버/약속 포함)를 조회합니다. */
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

    /** 현재 사용자를 팀에 가입시킵니다. */
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

    /** 현재 사용자의 팀 전용 프로필을 설정/수정합니다. */
    @Transactional
    public void setupMyTeamProfile(UUID teamId, TeamProfileSetupRequestDto request) {
        Member current = getCurrentMemberWithProfile();
        getTeam(teamId);

        TeamMembers membership = requireMembership(teamId, current.getId());
        membership.updateTeamProfile(request.teamNickName(), request.teamProfileImage());
    }

    /** 현재 사용자를 팀에서 탈퇴 처리합니다. */
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

    /** 팀장 권한으로 팀장 위임을 수행합니다. */
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

    /** 팀장 권한으로 팀원을 강제 추방합니다. */
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

    /** 현재 로그인 사용자의 프로필 설정 완료 여부를 확인하고 반환합니다. */
    private Member getCurrentMemberWithProfile() {
        Member current = currentMemberProvider.getCurrentMember();
        if (!current.isProfileSetupCompleted()) {
            throw new BusinessException(ErrorCode.PROFILE_SETUP_REQUIRED);
        }
        return current;
    }

    /** 팀 엔티티를 조회합니다. */
    private Team getTeam(UUID teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    /** 팀 멤버십을 검증하고 반환합니다. */
    private TeamMembers requireMembership(UUID teamId, Long memberId) {
        return teamMembersRepository.findByTeamIdAndMemberId(teamId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM));
    }

    /** 팀장 권한 여부를 검증합니다. */
    private void requireLeader(TeamMembers membership) {
        if (membership.getTeamRole() != TeamRole.ADMIN) {
            throw new BusinessException(ErrorCode.NOT_A_LEADER_OF_TEAM);
        }
    }

    /** 팀 이름을 공백 정리 후 검증합니다. */
    private String normalizeTeamName(String teamName) {
        if (teamName == null || teamName.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return teamName.trim();
    }
}
