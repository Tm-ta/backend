package com.example.tmta.service;

import com.example.tmta.dto.team.DetailTeamList;
import com.example.tmta.dto.team.TeamListResponseDto;
import com.example.tmta.dto.team.TeamSaveRequestDto;
import com.example.tmta.dto.team.TeamSaveResponseDto;
import com.example.tmta.entity.Member;
import com.example.tmta.entity.Team;
import com.example.tmta.entity.TeamMembers;
import com.example.tmta.entity.type.InviteState;
import com.example.tmta.entity.type.MemberRole;
import com.example.tmta.entity.type.TeamRole;
import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import com.example.tmta.repository.MemberRepository;
import com.example.tmta.repository.TeamMembersRepository;
import com.example.tmta.repository.TeamRepository;
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

    @Transactional
    public void kickMember(java.util.UUID teamId, Long memberId) {
        // TODO : Get Member From Security Context Holder
        Member leader = memberRepository.findByEmail("test@test.com").orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );

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
        // TODO : Get Member From Security Context Holder
        Member leader = memberRepository.findByEmail("test@test.com").orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );

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
        // TODO : Get Member From Security Context Holder
        Member member = memberRepository.findByEmail("test@test.com").orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );

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
    public void joinTeam(java.util.UUID teamId, com.example.tmta.dto.team.RequestJoinDto request) {
        // TODO : Get Member From Security Context Holder
        Member member = memberRepository.findByEmail("test@test.com").orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );

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
                .build();
        teamMembersRepository.save(teamMembers);
    }

    @Transactional(readOnly = true)
    public DetailTeamList getDetailTeam(java.util.UUID teamId) {
        Team team = teamRepository.findById(teamId).orElseThrow(
                () -> new BusinessException(ErrorCode.TEAM_NOT_FOUND)
        );
        return new DetailTeamList(team);
    }

    @Transactional(readOnly = true)
    public TeamListResponseDto getTeamList() {
        // TODO : Get Member From Security Context Holder
        Member member = memberRepository.findByEmail("test@test.com").orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );

        List<TeamMembers> teamMembers = teamMembersRepository.findAllByMember(member);
        return new TeamListResponseDto(teamMembers);
    }

    @Transactional
    public TeamSaveResponseDto createTeam(TeamSaveRequestDto requestDto) {
        // TODO : Get Member From Security Context Holder
        // Member member = Member.builder()
        //         .email("test@test.com")
        //         .name("test")
        //         .nickName("test")
        //         .role(MemberRole.GENERAL)
        //         .build();
        // memberRepository.save(member);

        Team team = requestDto.toEntity();
        teamRepository.save(team);

        // TeamMembers teamMembers = TeamMembers.builder()
        //         .team(team)
        //         .member(member)
        //         .inviteState(InviteState.ACCEPT)
        //         .teamRole(TeamRole.ADMIN)
        //         .build();
        // teamMembersRepository.save(teamMembers);

        // team.addTeamMember(teamMembers);
        return new TeamSaveResponseDto(team);
    }

    public void createTestMember() {
        Member member = Member.builder()
                .email("test@test.com")
                .name("test")
                .nickName("test")
                .role(MemberRole.GENERAL)
                .build();
        memberRepository.save(member);
    }
}
