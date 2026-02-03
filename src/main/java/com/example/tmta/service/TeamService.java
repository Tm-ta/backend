package com.example.tmta.service;

import com.example.tmta.dto.team.TeamSaveRequestDto;
import com.example.tmta.dto.team.TeamSaveResponseDto;
import com.example.tmta.entity.Member;
import com.example.tmta.entity.Team;
import com.example.tmta.entity.TeamMembers;
import com.example.tmta.entity.type.InviteState;
import com.example.tmta.entity.type.MemberRole;
import com.example.tmta.entity.type.TeamRole;
import com.example.tmta.repository.MemberRepository;
import com.example.tmta.repository.TeamMembersRepository;
import com.example.tmta.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final TeamMembersRepository teamMembersRepository;

    @Transactional
    public TeamSaveResponseDto createTeam(TeamSaveRequestDto requestDto, Long memberId) {
        // Find the member who is creating the team
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));

        // Create the team
        Team team = requestDto.toEntity();
        teamRepository.save(team);

        // Add the creator as a team member
        TeamMembers teamMembers = TeamMembers.builder()
                .team(team)
                .member(member)
                .inviteState(InviteState.ACCEPT)
                .teamRole(TeamRole.ADMIN)
                .build();
        teamMembersRepository.save(teamMembers);

        team.addTeamMember(teamMembers);

        return new TeamSaveResponseDto(team);
    }

    @Transactional
    public Member createTestMember() {
        Member member = Member.builder()
                .name("test")
                .nickName("test")
                .email("test@test.com")
                .authProvider("test")
                .providerId("test")
                .pushAlarmAgree(true)
                .role(MemberRole.GENERAL)
                .build();
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public com.example.tmta.dto.team.DetailTeamList getDetailTeam(java.util.UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + teamId));
        return new com.example.tmta.dto.team.DetailTeamList(team);
    }

    @Transactional(readOnly = true)
    public com.example.tmta.dto.team.TeamListResponseDto getTeamList() {
        return new com.example.tmta.dto.team.TeamListResponseDto(teamRepository.findAll());
    }

    @Transactional
    public Long joinTeam(java.util.UUID teamId, com.example.tmta.dto.team.RequestJoinDto request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + teamId));
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + request.getMemberId()));

        teamMembersRepository.findByTeamAndMember(team, member).ifPresent(teamMembers -> {
            throw new IllegalArgumentException("Member is already in the team");
        });

        TeamMembers teamMembers = TeamMembers.builder()
                .team(team)
                .member(member)
                .inviteState(com.example.tmta.entity.type.InviteState.PENDING)
                .teamRole(TeamRole.GENERAL)
                .build();
        teamMembersRepository.save(teamMembers);
        return teamMembers.getId();
    }

    @Transactional
    public void kickMember(java.util.UUID teamId, Long memberId, Long leaderId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + teamId));
        Member memberToKick = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));
        Member leader = memberRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + leaderId));

        TeamMembers leaderTeamMembers = teamMembersRepository.findByTeamAndMember(team, leader)
                .orElseThrow(() -> new IllegalArgumentException("Leader is not in the team"));

        if (leaderTeamMembers.getTeamRole() != TeamRole.ADMIN) {
            throw new IllegalArgumentException("Only the leader can kick members");
        }

        TeamMembers memberToKickTeamMembers = teamMembersRepository.findByTeamAndMember(team, memberToKick)
                .orElseThrow(() -> new IllegalArgumentException("Member to kick is not in the team"));

        teamMembersRepository.delete(memberToKickTeamMembers);
    }

    @Transactional
    public void delegateLeader(java.util.UUID teamId, Long memberId, Long leaderId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + teamId));
        Member newLeader = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));
        Member currentLeader = memberRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + leaderId));

        TeamMembers currentLeaderTeamMembers = teamMembersRepository.findByTeamAndMember(team, currentLeader)
                .orElseThrow(() -> new IllegalArgumentException("Leader is not in the team"));

        if (currentLeaderTeamMembers.getTeamRole() != TeamRole.ADMIN) {
            throw new IllegalArgumentException("Only the leader can delegate the role");
        }

        TeamMembers newLeaderTeamMembers = teamMembersRepository.findByTeamAndMember(team, newLeader)
                .orElseThrow(() -> new IllegalArgumentException("New leader is not in the team"));

        currentLeaderTeamMembers.updateTeamRole(TeamRole.GENERAL);
        newLeaderTeamMembers.updateTeamRole(TeamRole.ADMIN);
    }

    @Transactional
    public void exitTeam(java.util.UUID teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + teamId));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));

        TeamMembers teamMembers = teamMembersRepository.findByTeamAndMember(team, member)
                .orElseThrow(() -> new IllegalArgumentException("Member is not in the team"));

        if (teamMembers.getTeamRole() == TeamRole.ADMIN) {
            java.util.List<TeamMembers> teamMembersList = teamMembersRepository.findAllByTeam(team);
            if (teamMembersList.size() > 1) {
                throw new IllegalStateException("Leader cannot exit the team if there are other members");
            }
        }

        teamMembersRepository.delete(teamMembers);

        if (teamMembersRepository.findAllByTeam(team).isEmpty()) {
            teamRepository.delete(team);
        }
    }
}
