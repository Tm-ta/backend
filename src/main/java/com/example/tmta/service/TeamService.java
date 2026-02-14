package com.example.tmta.service;

import com.example.tmta.dto.team.DetailTeamList;
import com.example.tmta.dto.team.TeamProfileSetupRequestDto;
import com.example.tmta.dto.team.TeamListResponseDto;
import com.example.tmta.dto.team.TeamSaveRequestDto;
import com.example.tmta.dto.team.TeamSaveResponseDto;
import com.example.tmta.entity.Member;
import com.example.tmta.entity.Team;
import com.example.tmta.entity.TeamMembers;
import com.example.tmta.entity.type.InviteState;
import com.example.tmta.entity.type.TeamRole;
import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import com.example.tmta.repository.MemberRepository;
import com.example.tmta.repository.TeamMembersRepository;
import com.example.tmta.repository.TeamRepository;
import com.example.tmta.security.CurrentMemberProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final TeamMembersRepository teamMembersRepository;
    private final CurrentMemberProvider currentMemberProvider;

    @Transactional
    public void kickMember(java.util.UUID teamId, Long memberId) {
        Member leader = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(leader);

        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new BusinessException(ErrorCode.TEAM_NOT_FOUND)
        );

        TeamMembers leaderTeamMembers = teamMembersRepository.findByTeamAndMember(team, leader).orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM)
        );

        if (!leaderTeamMembers.getTeamRole().equals(TeamRole.ADMIN)) {
            throw new BusinessException(ErrorCode.NOT_A_LEADER_OF_TEAM);
        }

        Member memberToKick = memberRepository.findById(memberId).orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );

        if (leader.equals(memberToKick)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_LEADER);
        }

        TeamMembers memberToKickTeamMembers = teamMembersRepository.findByTeamAndMember(team, memberToKick).orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM)
        );

        teamMembersRepository.delete(memberToKickTeamMembers);
    }

    @Transactional
    public void delegateLeader(java.util.UUID teamId, Long memberId) {
        Member leader = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(leader);

        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new BusinessException(ErrorCode.TEAM_NOT_FOUND)
        );

        TeamMembers leaderTeamMembers = teamMembersRepository.findByTeamAndMember(team, leader).orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM)
        );

        if (!leaderTeamMembers.getTeamRole().equals(TeamRole.ADMIN)) {
            throw new BusinessException(ErrorCode.NOT_A_LEADER_OF_TEAM);
        }

        Member newLeader = memberRepository.findById(memberId).orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );
        TeamMembers newLeaderTeamMembers = teamMembersRepository.findByTeamAndMember(team, newLeader).orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM)
        );

        leaderTeamMembers.updateTeamRole(TeamRole.GENERAL);
        newLeaderTeamMembers.updateTeamRole(TeamRole.ADMIN);
    }

    @Transactional
    public void exitTeam(java.util.UUID teamId) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new BusinessException(ErrorCode.TEAM_NOT_FOUND)
        );

        TeamMembers teamMembers = teamMembersRepository.findByTeamAndMember(team, member).orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM)
        );

        if (teamMembers.getTeamRole().equals(TeamRole.ADMIN)) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_EXIT_TEAM);
        }

        teamMembersRepository.delete(teamMembers);
    }

    @Transactional
    public void joinTeam(java.util.UUID teamId) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new BusinessException(ErrorCode.TEAM_NOT_FOUND)
        );

        if (teamMembersRepository.findByTeamAndMember(team, member).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED_TEAM);
        }

        TeamMembers teamMembers = TeamMembers.builder()
                .team(team)
                .member(member)
                .inviteState(InviteState.PENDING)
                .teamRole(TeamRole.GENERAL)
                .teamNickName(null)
                .teamProfileImage(null)
                .teamProfileSetupCompleted(false)
                .build();
        teamMembersRepository.save(teamMembers);
    }

    @Transactional
    public void setupMyTeamProfile(java.util.UUID teamId, TeamProfileSetupRequestDto request) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new BusinessException(ErrorCode.TEAM_NOT_FOUND)
        );

        TeamMembers teamMembers = teamMembersRepository.findByTeamAndMember(team, member).orElseThrow(
                () -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM)
        );

        // join 이후 최초 설정/수정 모두 동일 API로 처리합니다.
        teamMembers.updateTeamProfile(request.getTeamNickName(), request.getTeamProfileImage());
    }

    @Transactional(readOnly = true)
    public DetailTeamList getDetailTeam(java.util.UUID teamId) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new BusinessException(ErrorCode.TEAM_NOT_FOUND)
        );
        return new DetailTeamList(team);
    }

    @Transactional(readOnly = true)
    public TeamListResponseDto getTeamList() {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        List<TeamMembers> teamMembers = teamMembersRepository.findAllByMember(member);
        return new TeamListResponseDto(teamMembers);
    }

    @Transactional
    public TeamSaveResponseDto createTeam(TeamSaveRequestDto requestDto) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = requestDto.toEntity();
        teamRepository.save(team);

        TeamMembers teamMembers = TeamMembers.builder()
                .team(team)
                .member(member)
                .inviteState(InviteState.ACCEPT)
                .teamRole(TeamRole.ADMIN)
                .teamNickName(member.getNickName())
                .teamProfileImage(member.getProfileImage())
                .teamProfileSetupCompleted(true)
                .build();
        teamMembersRepository.save(teamMembers);

        team.addTeamMember(teamMembers);
        return new TeamSaveResponseDto(team);
    }

    private void requireProfileSetupCompleted(Member member) {
        if (!member.isProfileSetupCompleted()) {
            throw new BusinessException(ErrorCode.PROFILE_SETUP_REQUIRED);
        }
    }
}
