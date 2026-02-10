package com.example.tmta.config;

import com.example.tmta.entity.Appointment;
import com.example.tmta.entity.Member;
import com.example.tmta.entity.Team;
import com.example.tmta.entity.TeamMembers;
import com.example.tmta.entity.type.AppointmentState;
import com.example.tmta.entity.type.InviteState;
import com.example.tmta.entity.type.MemberRole;
import com.example.tmta.entity.type.TeamRole;
import com.example.tmta.repository.AppointmentRepository;
import com.example.tmta.repository.MemberRepository;
import com.example.tmta.repository.TeamMembersRepository;
import com.example.tmta.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final TeamMembersRepository teamMembersRepository;
    private final AppointmentRepository appointmentRepository; // Inject AppointmentRepository

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Create initial members
        Member testMember = memberRepository.findByEmail("test@test.com").orElseGet(() -> {
            Member member = Member.builder()
                    .email("test@test.com")
                    .name("Test User")
                    .nickName("Tester")
                    .role(MemberRole.GENERAL)
                    .build();
            return memberRepository.save(member);
        });

        Member member1 = memberRepository.findByEmail("member1@test.com").orElseGet(() -> {
            Member member = Member.builder()
                    .email("member1@test.com")
                    .name("Member One")
                    .nickName("One")
                    .role(MemberRole.GENERAL)
                    .build();
            return memberRepository.save(member);
        });

        Member member2 = memberRepository.findByEmail("member2@test.com").orElseGet(() -> {
            Member member = Member.builder()
                    .email("member2@test.com")
                    .name("Member Two")
                    .nickName("Two")
                    .role(MemberRole.GENERAL)
                    .build();
            return memberRepository.save(member);
        });

        // --- Team 1: Team Alpha ---
        if (teamRepository.findByName("Team Alpha").isEmpty()) {
            Team teamAlpha = Team.builder()
                    .name("Team Alpha")
                    .profileImage("team-alpha.png")
                    .build();
            teamRepository.save(teamAlpha);

            // testMember is ADMIN of Team Alpha
            TeamMembers tmAlphaAdmin = TeamMembers.builder()
                    .team(teamAlpha)
                    .member(testMember)
                    .inviteState(InviteState.ACCEPT)
                    .teamRole(TeamRole.ADMIN)
                    .build();
            teamMembersRepository.save(tmAlphaAdmin);
            teamAlpha.addTeamMember(tmAlphaAdmin);

            // member1 is GENERAL of Team Alpha
            TeamMembers tmAlphaMember1 = TeamMembers.builder()
                    .team(teamAlpha)
                    .member(member1)
                    .inviteState(InviteState.ACCEPT)
                    .teamRole(TeamRole.GENERAL)
                    .build();
            teamMembersRepository.save(tmAlphaMember1);
            teamAlpha.addTeamMember(tmAlphaMember1);

            // Appointments for Team Alpha
            Appointment app1 = Appointment.builder()
                    .team(teamAlpha)
                    .name("Alpha Project Kickoff")
                    .description("Initial meeting for the Alpha project")
                    .address("Online")
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(11, 0))
                    .state(AppointmentState.CREATING)
                    .build();
            appointmentRepository.save(app1);

            Appointment app2 = Appointment.builder()
                    .team(teamAlpha)
                    .name("Alpha Weekly Sync")
                    .description("Regular weekly meeting")
                    .address("Conference Room A")
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(15, 0))
                    .state(AppointmentState.CONFIRMED)
                    .build();
            appointmentRepository.save(app2);
        }

        // --- Team 2: Team Beta ---
        if (teamRepository.findByName("Team Beta").isEmpty()) {
            Team teamBeta = Team.builder()
                    .name("Team Beta")
                    .profileImage("team-beta.png")
                    .build();
            teamRepository.save(teamBeta);

            // member1 is ADMIN of Team Beta
            TeamMembers tmBetaAdmin = TeamMembers.builder()
                    .team(teamBeta)
                    .member(member1)
                    .inviteState(InviteState.ACCEPT)
                    .teamRole(TeamRole.ADMIN)
                    .build();
            teamMembersRepository.save(tmBetaAdmin);
            teamBeta.addTeamMember(tmBetaAdmin);

            // testMember is GENERAL of Team Beta
            TeamMembers tmBetaTestMember = TeamMembers.builder()
                    .team(teamBeta)
                    .member(testMember)
                    .inviteState(InviteState.ACCEPT)
                    .teamRole(TeamRole.GENERAL)
                    .build();
            teamMembersRepository.save(tmBetaTestMember);
            teamBeta.addTeamMember(tmBetaTestMember);

            // member2 is GENERAL of Team Beta
            TeamMembers tmBetaMember2 = TeamMembers.builder()
                    .team(teamBeta)
                    .member(member2)
                    .inviteState(InviteState.PENDING) // Pending invite
                    .teamRole(TeamRole.GENERAL)
                    .build();
            teamMembersRepository.save(tmBetaMember2);
            teamBeta.addTeamMember(tmBetaMember2);

            // Appointments for Team Beta
            Appointment app3 = Appointment.builder()
                    .team(teamBeta)
                    .name("Beta Feature Discussion")
                    .description("Discuss new features for Beta project")
                    .address("Zoom Call")
                    .startTime(LocalTime.of(9, 30))
                    .endTime(LocalTime.of(10, 30))
                    .state(AppointmentState.CANCELED)
                    .build();
            appointmentRepository.save(app3);
        }
    }
}
