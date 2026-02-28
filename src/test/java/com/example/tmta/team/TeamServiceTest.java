package com.example.tmta.team;

import com.example.tmta.team.dto.DetailTeamList;
import com.example.tmta.team.dto.TeamListResponseDto;
import com.example.tmta.team.dto.TeamProfileSetupRequestDto;
import com.example.tmta.team.dto.TeamSaveRequestDto;
import com.example.tmta.member.entity.Member;
import com.example.tmta.team.entity.Team;
import com.example.tmta.team.entity.TeamMembers;
import com.example.tmta.member.entity.type.AuthProvider;
import com.example.tmta.member.entity.type.MemberRole;
import com.example.tmta.team.entity.type.TeamRole;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.example.tmta.appointment.repository.AppointmentRepository;
import com.example.tmta.member.repository.MemberRepository;
import com.example.tmta.team.repository.TeamMembersRepository;
import com.example.tmta.team.repository.TeamRepository;
import com.example.tmta.common.security.CurrentMemberProvider;
import com.example.tmta.team.TeamQueryAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private TeamMembersRepository teamMembersRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private CurrentMemberProvider currentMemberProvider;
    @Mock
    private TeamQueryAssembler teamQueryAssembler;

    @InjectMocks
    private TeamService teamService;

    private Member currentMember;

    @BeforeEach
    void setUp() {
        currentMember = member(1L, true);
    }

    @Nested
    @DisplayName("getTeamList")
    class GetTeamList {

        @Test
        @DisplayName("소속 팀이 없으면 빈 목록 반환")
        void returnsEmptyWhenNoMembership() {
            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamMembersRepository.findAllByMemberId(currentMember.getId())).thenReturn(List.of());

            TeamListResponseDto response = teamService.getTeamList();

            assertThat(response.teamList()).isEmpty();
            verify(teamQueryAssembler, never()).toTeamListResponse(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("프로필 미완료 사용자는 PROFILE_SETUP_REQUIRED 예외")
        void profileSetupRequired() {
            when(currentMemberProvider.getCurrentMember()).thenReturn(member(1L, false));

            assertThatThrownBy(() -> teamService.getTeamList())
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PROFILE_SETUP_REQUIRED);
        }
    }

    @Nested
    @DisplayName("createTeam")
    class CreateTeam {

        @Test
        @DisplayName("팀 생성 성공 시 팀/리더 멤버십 저장")
        void createTeamSuccess() {
            TeamSaveRequestDto request = new TeamSaveRequestDto(" Backend Team ", false, true);
            UUID createdTeamId = UUID.randomUUID();

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
                Team saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", createdTeamId);
                return saved;
            });

            var response = teamService.createTeam(request);

            assertThat(response.groupId()).isEqualTo(createdTeamId);

            ArgumentCaptor<Team> teamCaptor = ArgumentCaptor.forClass(Team.class);
            verify(teamRepository).save(teamCaptor.capture());
            assertThat(teamCaptor.getValue().getName()).isEqualTo("Backend Team");

            ArgumentCaptor<TeamMembers> membershipCaptor = ArgumentCaptor.forClass(TeamMembers.class);
            verify(teamMembersRepository).save(membershipCaptor.capture());
            assertThat(membershipCaptor.getValue().getTeamId()).isEqualTo(createdTeamId);
            assertThat(membershipCaptor.getValue().getMemberId()).isEqualTo(currentMember.getId());
            assertThat(membershipCaptor.getValue().getTeamRole()).isEqualTo(TeamRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("joinTeam")
    class JoinTeam {

        @Test
        @DisplayName("이미 가입한 팀이면 ALREADY_JOINED_TEAM 예외")
        void alreadyJoined() {
            UUID teamId = UUID.randomUUID();

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(Team.builder().name("A").build()));
            when(teamMembersRepository.existsByTeamIdAndMemberId(teamId, currentMember.getId())).thenReturn(true);

            assertThatThrownBy(() -> teamService.joinTeam(teamId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ALREADY_JOINED_TEAM);
        }
    }

    @Nested
    @DisplayName("setupMyTeamProfile")
    class SetupMyTeamProfile {

        @Test
        @DisplayName("멤버십이 있으면 팀 프로필 갱신")
        void setupProfileSuccess() {
            UUID teamId = UUID.randomUUID();
            TeamMembers membership = TeamMembers.createGeneral(teamId, currentMember.getId());

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(Team.builder().name("A").build()));
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId())).thenReturn(Optional.of(membership));

            teamService.setupMyTeamProfile(teamId, new TeamProfileSetupRequestDto("새닉", "tmta-assets", "team-profile/1/img.png"));

            assertThat(membership.getTeamNickName()).isEqualTo("새닉");
            assertThat(membership.getTeamProfileImageBucket()).isEqualTo("tmta-assets");
            assertThat(membership.getTeamProfileImageKey()).isEqualTo("team-profile/1/img.png");
            assertThat(membership.isTeamProfileSetupCompleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("exitTeam")
    class ExitTeam {

        @Test
        @DisplayName("팀장은 탈퇴 불가")
        void leaderCannotExit() {
            UUID teamId = UUID.randomUUID();
            TeamMembers leaderMembership = TeamMembers.createLeader(teamId, currentMember.getId(), "nick", "tmta-assets", "member-profile/1/img.png");

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(Team.builder().name("A").build()));
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId())).thenReturn(Optional.of(leaderMembership));

            assertThatThrownBy(() -> teamService.exitTeam(teamId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.LEADER_CANNOT_EXIT_TEAM);
        }
    }

    @Nested
    @DisplayName("delegateLeader")
    class DelegateLeader {

        @Test
        @DisplayName("현재 사용자가 리더가 아니면 NOT_A_LEADER_OF_TEAM 예외")
        void notLeader() {
            UUID teamId = UUID.randomUUID();
            TeamMembers generalMembership = TeamMembers.createGeneral(teamId, currentMember.getId());

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(Team.builder().name("A").build()));
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId())).thenReturn(Optional.of(generalMembership));

            assertThatThrownBy(() -> teamService.delegateLeader(teamId, 2L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.NOT_A_LEADER_OF_TEAM);
        }

        @Test
        @DisplayName("리더 위임 성공 시 역할 교체")
        void delegateSuccess() {
            UUID teamId = UUID.randomUUID();
            TeamMembers leaderMembership = TeamMembers.createLeader(teamId, currentMember.getId(), "leader", null, null);
            TeamMembers targetMembership = TeamMembers.createGeneral(teamId, 2L);

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(Team.builder().name("A").build()));
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId())).thenReturn(Optional.of(leaderMembership));
            when(memberRepository.findById(2L)).thenReturn(Optional.of(member(2L, true)));
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, 2L)).thenReturn(Optional.of(targetMembership));

            teamService.delegateLeader(teamId, 2L);

            assertThat(leaderMembership.getTeamRole()).isEqualTo(TeamRole.GENERAL);
            assertThat(targetMembership.getTeamRole()).isEqualTo(TeamRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("kickMember")
    class KickMember {

        @Test
        @DisplayName("리더가 자기 자신을 강퇴하려 하면 CANNOT_KICK_LEADER 예외")
        void cannotKickSelf() {
            UUID teamId = UUID.randomUUID();
            TeamMembers leaderMembership = TeamMembers.createLeader(teamId, currentMember.getId(), "leader", null, null);

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(Team.builder().name("A").build()));
            when(teamMembersRepository.findByTeamIdAndMemberId(eq(teamId), eq(currentMember.getId())))
                    .thenReturn(Optional.of(leaderMembership));

            assertThatThrownBy(() -> teamService.kickMember(teamId, currentMember.getId()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CANNOT_KICK_LEADER);
        }
    }

    @Nested
    @DisplayName("getDetailTeam")
    class GetDetailTeam {

        @Test
        @DisplayName("상세 조회 시 어셈블러 결과를 반환")
        void getDetailSuccess() {
            UUID teamId = UUID.randomUUID();
            Team team = Team.builder().name("A").build();
            TeamMembers membership = TeamMembers.createGeneral(teamId, currentMember.getId());
            DetailTeamList expected = new DetailTeamList(teamId, "A", 1L, null, List.of(), List.of());

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId())).thenReturn(Optional.of(membership));
            when(teamMembersRepository.findAllByTeamId(teamId)).thenReturn(List.of(membership));
            when(memberRepository.findAllById(List.of(currentMember.getId()))).thenReturn(List.of(currentMember));
            when(appointmentRepository.findAllByTeamId(teamId)).thenReturn(List.of());
            when(teamQueryAssembler.toDetailTeamList(eq(team), any(), any(), any())).thenReturn(expected);

            DetailTeamList result = teamService.getDetailTeam(teamId);

            assertThat(result).isSameAs(expected);
        }
    }

    private static Member member(Long id, boolean profileSetupCompleted) {
        return Member.builder()
                .id(id)
                .email(id + "@test.com")
                .password("encoded")
                .authProvider(AuthProvider.LOCAL)
                .role(MemberRole.GENERAL)
                .profileSetupCompleted(profileSetupCompleted)
                .emailVerified(true)
                .build();
    }
}
