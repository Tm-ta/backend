package com.example.tmta.common.config;

import com.example.tmta.appointment.entity.Appointment;
import com.example.tmta.member.entity.Member;
import com.example.tmta.team.entity.Team;
import com.example.tmta.team.entity.TeamMembers;
import com.example.tmta.appointment.entity.type.AppointmentState;
import com.example.tmta.member.entity.type.AuthProvider;
import com.example.tmta.member.entity.type.MemberRole;
import com.example.tmta.team.entity.type.TeamRole;
import com.example.tmta.appointment.repository.AppointmentRepository;
import com.example.tmta.member.repository.MemberRepository;
import com.example.tmta.team.repository.TeamMembersRepository;
import com.example.tmta.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final TeamMembersRepository teamMembersRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Create initial members
        Member testMember = memberRepository.findByEmail("test@test.com").orElseGet(() -> {
            Member member = Member.builder()
                    .email("test@test.com")
                    .password(passwordEncoder.encode("Test1234!"))
                    .name("Test User")
                    .nickName("Tester")
                    .profileSetupCompleted(true)
                    .authProvider(AuthProvider.LOCAL)
                    .emailVerified(true)
                    .role(MemberRole.GENERAL)
                    .build();
            return memberRepository.save(member);
        });

        Member member1 = memberRepository.findByEmail("member1@test.com").orElseGet(() -> {
            Member member = Member.builder()
                    .email("member1@test.com")
                    .password(passwordEncoder.encode("Test1234!"))
                    .name("Member One")
                    .nickName("One")
                    .profileSetupCompleted(true)
                    .authProvider(AuthProvider.LOCAL)
                    .emailVerified(true)
                    .role(MemberRole.GENERAL)
                    .build();
            return memberRepository.save(member);
        });

        Member member2 = memberRepository.findByEmail("member2@test.com").orElseGet(() -> {
            Member member = Member.builder()
                    .email("member2@test.com")
                    .password(passwordEncoder.encode("Test1234!"))
                    .name("Member Two")
                    .nickName("Two")
                    .profileSetupCompleted(true)
                    .authProvider(AuthProvider.LOCAL)
                    .emailVerified(true)
                    .role(MemberRole.GENERAL)
                    .build();
            return memberRepository.save(member);
        });

        // --- Team 1: Team Alpha ---
        if (teamRepository.findByName("Team Alpha").isEmpty()) {
            Team teamAlpha = Team.builder()
                    .name("Team Alpha")
                    .profileImageBucket("tmta-dev-assets")
                    .profileImageKey("team-profile/team-alpha.png")
                    .build();
            teamRepository.save(teamAlpha);

            // testMember is ADMIN of Team Alpha
            TeamMembers tmAlphaAdmin = TeamMembers.builder()
                    .teamId(teamAlpha.getId())
                    .memberId(testMember.getId())
                    .teamRole(TeamRole.ADMIN)
                    .teamNickName(testMember.getNickName())
                    .teamProfileImageBucket(testMember.getProfileImageBucket())
                    .teamProfileImageKey(testMember.getProfileImageKey())
                    .teamProfileSetupCompleted(true)
                    .build();
            teamMembersRepository.save(tmAlphaAdmin);

            // member1 is GENERAL of Team Alpha
            TeamMembers tmAlphaMember1 = TeamMembers.builder()
                    .teamId(teamAlpha.getId())
                    .memberId(member1.getId())
                    .teamRole(TeamRole.GENERAL)
                    .teamNickName(member1.getNickName())
                    .teamProfileImageBucket(member1.getProfileImageBucket())
                    .teamProfileImageKey(member1.getProfileImageKey())
                    .teamProfileSetupCompleted(true)
                    .build();
            teamMembersRepository.save(tmAlphaMember1);

            // Appointments for Team Alpha
            Appointment app1 = Appointment.builder()
                    .teamId(teamAlpha.getId())
                    .createdByMemberId(testMember.getId())
                    .name("Alpha Project Kickoff")
                    .description("Initial meeting for the Alpha project")
                    .address("Online")
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(11, 0))
                    .state(AppointmentState.CREATING)
                    .build();
            appointmentRepository.save(app1);

            Appointment app2 = Appointment.builder()
                    .teamId(teamAlpha.getId())
                    .createdByMemberId(member1.getId())
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
                    .profileImageBucket("tmta-dev-assets")
                    .profileImageKey("team-profile/team-beta.png")
                    .build();
            teamRepository.save(teamBeta);

            // member1 is ADMIN of Team Beta
            TeamMembers tmBetaAdmin = TeamMembers.builder()
                    .teamId(teamBeta.getId())
                    .memberId(member1.getId())
                    .teamRole(TeamRole.ADMIN)
                    .teamNickName(member1.getNickName())
                    .teamProfileImageBucket(member1.getProfileImageBucket())
                    .teamProfileImageKey(member1.getProfileImageKey())
                    .teamProfileSetupCompleted(true)
                    .build();
            teamMembersRepository.save(tmBetaAdmin);

            // testMember is GENERAL of Team Beta
            TeamMembers tmBetaTestMember = TeamMembers.builder()
                    .teamId(teamBeta.getId())
                    .memberId(testMember.getId())
                    .teamRole(TeamRole.GENERAL)
                    .teamNickName(testMember.getNickName())
                    .teamProfileImageBucket(testMember.getProfileImageBucket())
                    .teamProfileImageKey(testMember.getProfileImageKey())
                    .teamProfileSetupCompleted(true)
                    .build();
            teamMembersRepository.save(tmBetaTestMember);

            // member2 is GENERAL of Team Beta
            TeamMembers tmBetaMember2 = TeamMembers.builder()
                    .teamId(teamBeta.getId())
                    .memberId(member2.getId())
                    .teamRole(TeamRole.GENERAL)
                    .teamNickName(member2.getNickName())
                    .teamProfileImageBucket(member2.getProfileImageBucket())
                    .teamProfileImageKey(member2.getProfileImageKey())
                    .teamProfileSetupCompleted(true)
                    .build();
            teamMembersRepository.save(tmBetaMember2);

            // Appointments for Team Beta
            Appointment app3 = Appointment.builder()
                    .teamId(teamBeta.getId())
                    .createdByMemberId(member1.getId())
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
