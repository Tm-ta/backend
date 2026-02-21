package com.example.tmta.appointment;

import com.example.tmta.appointment.dto.AppointmentConfirmRequestDto;
import com.example.tmta.appointment.dto.AppointmentSaveRequestDto;
import com.example.tmta.appointment.dto.AppointmentTimeRegisterRequestDto;
import com.example.tmta.appointment.entity.Appointment;
import com.example.tmta.member.entity.Member;
import com.example.tmta.team.entity.TeamMembers;
import com.example.tmta.appointment.entity.type.AppointmentState;
import com.example.tmta.member.entity.type.AuthProvider;
import com.example.tmta.member.entity.type.MemberRole;
import com.example.tmta.team.entity.type.TeamRole;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.example.tmta.appointment.repository.AppointmentDateRepository;
import com.example.tmta.appointment.repository.AppointmentRepository;
import com.example.tmta.appointment.repository.AvailableTimeRepository;
import com.example.tmta.member.repository.MemberRepository;
import com.example.tmta.team.repository.TeamMembersRepository;
import com.example.tmta.team.repository.TeamRepository;
import com.example.tmta.common.security.CurrentMemberProvider;
import com.example.tmta.appointment.AppointmentQueryAssembler;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentDateRepository appointmentDateRepository;
    @Mock
    private AvailableTimeRepository availableTimeRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamMembersRepository teamMembersRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CurrentMemberProvider currentMemberProvider;
    @Mock
    private AppointmentSlotCalculator slotCalculator;
    @Mock
    private AppointmentCommandPolicy appointmentCommandPolicy;
    @Mock
    private AppointmentQueryAssembler appointmentQueryAssembler;

    @InjectMocks
    private AppointmentService appointmentService;

    private Member currentMember;
    private UUID teamId;
    private UUID appointmentId;

    @BeforeEach
    void setUp() {
        currentMember = member(1L, true);
        teamId = UUID.randomUUID();
        appointmentId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("createTeamAppointment")
    class CreateTeamAppointment {

        @Test
        @DisplayName("정상 입력이면 약속 생성 후 ID 반환")
        void createSuccess() {
            AppointmentSaveRequestDto request = new AppointmentSaveRequestDto(
                    "정기회의",
                    List.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2)),
                    LocalTime.of(10, 0),
                    LocalTime.of(12, 0),
                    false,
                    "desc",
                    LocalDateTime.of(2026, 2, 28, 23, 0)
            );

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.existsById(teamId)).thenReturn(true);
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId()))
                    .thenReturn(Optional.of(TeamMembers.createGeneral(teamId, currentMember.getId())));
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
                Appointment saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", appointmentId);
                return saved;
            });

            UUID result = appointmentService.createTeamAppointment(teamId, request);

            assertThat(result).isEqualTo(appointmentId);
            verify(appointmentCommandPolicy).validateAppointmentCommand(
                    request.name(), request.appointmentDates(), request.onlyDate(), request.startTime(), request.endTime()
            );

            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());
            assertThat(captor.getValue().getTeamId()).isEqualTo(teamId);
            assertThat(captor.getValue().getCreatedByMemberId()).isEqualTo(currentMember.getId());
            assertThat(captor.getValue().getState()).isEqualTo(AppointmentState.SCHEDULING);
        }

        @Test
        @DisplayName("팀이 없으면 TEAM_NOT_FOUND 예외")
        void teamNotFound() {
            AppointmentSaveRequestDto request = new AppointmentSaveRequestDto(
                    "정기회의", List.of(LocalDate.of(2026, 3, 1)), LocalTime.of(10, 0), LocalTime.of(12, 0), false, null, null
            );

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.existsById(teamId)).thenReturn(false);

            assertThatThrownBy(() -> appointmentService.createTeamAppointment(teamId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TEAM_NOT_FOUND);

            verify(appointmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("registerTeamAppointmentTime")
    class RegisterTeamAppointmentTime {

        @Test
        @DisplayName("요청 슬롯이 비어있으면 INVALID_INPUT_VALUE 예외")
        void invalidEmptySlots() {
            Appointment appointment = schedulingAppointment(teamId, currentMember.getId());

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.existsById(teamId)).thenReturn(true);
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId()))
                    .thenReturn(Optional.of(TeamMembers.createGeneral(teamId, currentMember.getId())));
            when(appointmentRepository.findDetailByIdAndTeamId(appointmentId, teamId)).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.registerTeamAppointmentTime(
                    teamId,
                    appointmentId,
                    new AppointmentTimeRegisterRequestDto(List.of())
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("약속 시간 범위를 벗어나면 APPOINTMENT_TIME_OUT_OF_RANGE 예외")
        void outOfRangeTime() {
            Appointment appointment = schedulingAppointment(teamId, currentMember.getId());

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.existsById(teamId)).thenReturn(true);
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId()))
                    .thenReturn(Optional.of(TeamMembers.createGeneral(teamId, currentMember.getId())));
            when(appointmentRepository.findDetailByIdAndTeamId(appointmentId, teamId)).thenReturn(Optional.of(appointment));

            AppointmentTimeRegisterRequestDto request = new AppointmentTimeRegisterRequestDto(List.of(
                    new AppointmentTimeRegisterRequestDto.TimeSlot(LocalDate.of(2026, 3, 1), LocalTime.of(9, 30))
            ));

            assertThatThrownBy(() -> appointmentService.registerTeamAppointmentTime(teamId, appointmentId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.APPOINTMENT_TIME_OUT_OF_RANGE);
        }

        @Test
        @DisplayName("유효 요청이면 사용자 가능시간을 기존 삭제 후 저장")
        void registerSuccess() {
            Appointment appointment = schedulingAppointment(teamId, currentMember.getId());

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.existsById(teamId)).thenReturn(true);
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId()))
                    .thenReturn(Optional.of(TeamMembers.createGeneral(teamId, currentMember.getId())));
            when(appointmentRepository.findDetailByIdAndTeamId(appointmentId, teamId)).thenReturn(Optional.of(appointment));
            when(slotCalculator.compressToRanges(List.of(LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 30))))
                    .thenReturn(List.of(
                            new AppointmentSlotCalculator.TimeRange(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                            new AppointmentSlotCalculator.TimeRange(LocalTime.of(11, 30), LocalTime.of(12, 0))
                    ));

            AppointmentTimeRegisterRequestDto request = new AppointmentTimeRegisterRequestDto(List.of(
                    new AppointmentTimeRegisterRequestDto.TimeSlot(LocalDate.of(2026, 3, 1), LocalTime.of(10, 0)),
                    new AppointmentTimeRegisterRequestDto.TimeSlot(LocalDate.of(2026, 3, 1), LocalTime.of(10, 30)),
                    new AppointmentTimeRegisterRequestDto.TimeSlot(LocalDate.of(2026, 3, 1), LocalTime.of(11, 30))
            ));

            appointmentService.registerTeamAppointmentTime(teamId, appointmentId, request);

            verify(availableTimeRepository).deleteAllByAppointmentDateAppointmentAndMemberId(appointment, currentMember.getId());
            verify(availableTimeRepository, times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("confirmTeamAppointment")
    class ConfirmTeamAppointment {

        @Test
        @DisplayName("관리 권한이 없으면 FORBIDDEN 예외")
        void forbiddenWhenNotManager() {
            Appointment appointment = schedulingAppointment(teamId, 999L);

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.existsById(teamId)).thenReturn(true);
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId()))
                    .thenReturn(Optional.of(TeamMembers.createGeneral(teamId, currentMember.getId())));
            when(appointmentRepository.findDetailByIdAndTeamId(appointmentId, teamId)).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.confirmTeamAppointment(
                    teamId,
                    appointmentId,
                    new AppointmentConfirmRequestDto(LocalDate.of(2026, 3, 1), LocalTime.of(10, 0), LocalTime.of(11, 0))
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("요청 날짜가 약속 후보 날짜에 없으면 APPOINTMENT_DATE_MISMATCH 예외")
        void dateMismatch() {
            Appointment appointment = schedulingAppointment(teamId, currentMember.getId());

            TeamMembers adminMembership = TeamMembers.createLeader(teamId, currentMember.getId(), "leader", null);
            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.existsById(teamId)).thenReturn(true);
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId())).thenReturn(Optional.of(adminMembership));
            when(appointmentRepository.findDetailByIdAndTeamId(appointmentId, teamId)).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.confirmTeamAppointment(
                    teamId,
                    appointmentId,
                    new AppointmentConfirmRequestDto(LocalDate.of(2026, 3, 5), LocalTime.of(10, 0), LocalTime.of(11, 0))
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.APPOINTMENT_DATE_MISMATCH);
        }

        @Test
        @DisplayName("확정 성공 시 finalTime 생성 및 상태 CONFIRMED")
        void confirmSuccess() {
            Appointment appointment = schedulingAppointment(teamId, currentMember.getId());
            TeamMembers adminMembership = TeamMembers.createLeader(teamId, currentMember.getId(), "leader", null);

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.existsById(teamId)).thenReturn(true);
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId())).thenReturn(Optional.of(adminMembership));
            when(appointmentRepository.findDetailByIdAndTeamId(appointmentId, teamId)).thenReturn(Optional.of(appointment));

            appointmentService.confirmTeamAppointment(
                    teamId,
                    appointmentId,
                    new AppointmentConfirmRequestDto(LocalDate.of(2026, 3, 1), LocalTime.of(10, 0), LocalTime.of(11, 0))
            );

            assertThat(appointment.getState()).isEqualTo(AppointmentState.CONFIRMED);
            assertThat(appointment.getConfirmedDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        }
    }

    @Nested
    @DisplayName("deadlineTeamAppointment")
    class DeadlineTeamAppointment {

        @Test
        @DisplayName("상태가 SCHEDULING/CREATING이 아니면 INVALID_APPOINTMENT_STATE 예외")
        void invalidState() {
            Appointment appointment = schedulingAppointment(teamId, currentMember.getId());
            appointment.updateState(AppointmentState.CONFIRMED);
            TeamMembers adminMembership = TeamMembers.createLeader(teamId, currentMember.getId(), "leader", null);

            when(currentMemberProvider.getCurrentMember()).thenReturn(currentMember);
            when(teamRepository.existsById(teamId)).thenReturn(true);
            when(teamMembersRepository.findByTeamIdAndMemberId(teamId, currentMember.getId())).thenReturn(Optional.of(adminMembership));
            when(appointmentRepository.findByIdAndTeamId(appointmentId, teamId)).thenReturn(Optional.of(appointment));

            assertThatThrownBy(() -> appointmentService.deadlineTeamAppointment(teamId, appointmentId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_APPOINTMENT_STATE);
        }
    }

    @Nested
    @DisplayName("guard")
    class Guard {

        @Test
        @DisplayName("프로필 미완료 사용자는 PROFILE_SETUP_REQUIRED 예외")
        void profileSetupRequired() {
            AppointmentSaveRequestDto request = new AppointmentSaveRequestDto(
                    "정기회의", List.of(LocalDate.of(2026, 3, 1)), LocalTime.of(10, 0), LocalTime.of(12, 0), false, null, null
            );
            when(currentMemberProvider.getCurrentMember()).thenReturn(member(1L, false));

            assertThatThrownBy(() -> appointmentService.createTeamAppointment(teamId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.PROFILE_SETUP_REQUIRED);
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

    private static Appointment schedulingAppointment(UUID teamId, Long createdByMemberId) {
        return Appointment.create(
                teamId,
                createdByMemberId,
                "정기회의",
                "desc",
                false,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                LocalDateTime.of(2026, 2, 28, 23, 0),
                List.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2))
        );
    }
}
