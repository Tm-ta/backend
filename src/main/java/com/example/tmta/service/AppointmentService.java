package com.example.tmta.service;

import com.example.tmta.dto.MemberInfo;
import com.example.tmta.dto.appointment.AppointmentCandidateFilter;
import com.example.tmta.dto.appointment.AppointmentConfirmRequestDto;
import com.example.tmta.dto.appointment.AppointmentDetailResponseDto;
import com.example.tmta.dto.appointment.AppointmentListResponseDto;
import com.example.tmta.dto.appointment.AppointmentSaveRequestDto;
import com.example.tmta.dto.appointment.AppointmentTimeRegisterRequestDto;
import com.example.tmta.dto.appointment.AppointmentTimeTableResponseDto;
import com.example.tmta.dto.appointment.AppointmentUpdateRequestDto;
import com.example.tmta.entity.Appointment;
import com.example.tmta.entity.AppointmentDate;
import com.example.tmta.entity.AvailableTime;
import com.example.tmta.entity.Member;
import com.example.tmta.entity.TeamMembers;
import com.example.tmta.entity.type.AppointmentState;
import com.example.tmta.entity.type.TeamRole;
import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import com.example.tmta.repository.AppointmentDateRepository;
import com.example.tmta.repository.AppointmentRepository;
import com.example.tmta.repository.AvailableTimeRepository;
import com.example.tmta.repository.MemberRepository;
import com.example.tmta.repository.TeamMembersRepository;
import com.example.tmta.repository.TeamRepository;
import com.example.tmta.security.CurrentMemberProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentDateRepository appointmentDateRepository;
    private final AvailableTimeRepository availableTimeRepository;
    private final TeamRepository teamRepository;
    private final TeamMembersRepository teamMembersRepository;
    private final MemberRepository memberRepository;
    private final CurrentMemberProvider currentMemberProvider;
    private final AppointmentSlotCalculator slotCalculator;
    private final AppointmentCommandPolicy appointmentCommandPolicy;

    @Transactional
    public UUID createTeamAppointment(UUID teamId, AppointmentSaveRequestDto request) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        requireMembership(teamId, current.getId());

        appointmentCommandPolicy.validateAppointmentCommand(
                request.name(), request.appointmentDates(), request.onlyDate(), request.startTime(), request.endTime()
        );

        Appointment appointment = Appointment.create(
                teamId,
                current.getId(),
                request.name(),
                request.description(),
                request.onlyDate(),
                request.startTime(),
                request.endTime(),
                request.deadlineDateTime(),
                request.appointmentDates()
        );

        return appointmentRepository.save(appointment).getId();
    }

    @Transactional(readOnly = true)
    public AppointmentDetailResponseDto getTeamAppointmentDetail(UUID teamId, UUID appointmentId) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        requireMembership(teamId, current.getId());

        Appointment appointment = getAppointmentDetail(teamId, appointmentId);
        List<TeamMembers> memberships = teamMembersRepository.findAllByTeamId(teamId);
        List<Long> memberIds = memberships.stream().map(TeamMembers::getMemberId).distinct().toList();
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member));

        List<LocalDate> dates = appointment.getAppointmentDateList().stream()
                .map(AppointmentDate::getDate)
                .sorted()
                .toList();
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (!dates.isEmpty()) {
            startDate = dates.get(0);
            endDate = dates.get(dates.size() - 1);
        }
        LocalDate confirmedDate = null;
        if (appointment.getState() == AppointmentState.CONFIRMED || appointment.getState() == AppointmentState.COMPLETE) {
            confirmedDate = appointment.getConfirmedDate();
        }
        return new AppointmentDetailResponseDto(
                appointment.getName(),
                resolveOrganizerName(appointment, memberMap, memberships),
                (long) memberships.size(),
                startDate,
                endDate,
                memberships.stream()
                        .map(membership -> toMemberInfo(membership, memberMap.get(membership.getMemberId())))
                        .toList(),
                appointment.getDescription(),
                appointment.getState(),
                confirmedDate
        );
    }

    @Transactional(readOnly = true)
    public AppointmentListResponseDto getTeamAppointmentCandidate(UUID teamId, UUID appointmentId, AppointmentCandidateFilter filter) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        requireMembership(teamId, current.getId());
        Appointment appointment = getAppointmentDetail(teamId, appointmentId);

        List<AppointmentSlotCalculator.SlotAggregate> filtered = slotCalculator.applyFilter(aggregateAvailableSlots(appointment), filter);
        return new AppointmentListResponseDto(appointmentId, filtered.stream().map(this::toAvailableDateTime).toList());
    }

    @Transactional(readOnly = true)
    public AppointmentTimeTableResponseDto getTeamAppointmentTimeTable(UUID teamId, UUID appointmentId, AppointmentCandidateFilter filter) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        requireMembership(teamId, current.getId());
        Appointment appointment = getAppointmentDetail(teamId, appointmentId);

        List<AppointmentSlotCalculator.SlotAggregate> filtered = slotCalculator.applyFilter(aggregateAvailableSlots(appointment), filter);
        return new AppointmentTimeTableResponseDto(appointmentId, filtered.stream().map(this::toTimeTableSlot).toList());
    }

    @Transactional
    public void deadlineTeamAppointment(UUID teamId, UUID appointmentId) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        TeamMembers membership = requireMembership(teamId, current.getId());
        Appointment appointment = getAppointment(teamId, appointmentId);
        requireCanManageAppointment(membership, appointment, current.getId());

        if (appointment.getState() != AppointmentState.SCHEDULING && appointment.getState() != AppointmentState.CREATING) {
            throw new BusinessException(ErrorCode.INVALID_APPOINTMENT_STATE);
        }
        appointment.closeScheduling();
    }

    @Transactional
    public void registerTeamAppointmentTime(UUID teamId, UUID appointmentId, AppointmentTimeRegisterRequestDto request) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        requireMembership(teamId, current.getId());
        Appointment appointment = getAppointmentDetail(teamId, appointmentId);

        if (request == null || request.timeSlots() == null || request.timeSlots().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (appointment.getState() != AppointmentState.SCHEDULING && appointment.getState() != AppointmentState.CREATING) {
            throw new BusinessException(ErrorCode.INVALID_APPOINTMENT_STATE);
        }

        Map<LocalDate, AppointmentDate> dateMap = appointment.getAppointmentDateList().stream()
                .collect(Collectors.toMap(AppointmentDate::getDate, d -> d));

        availableTimeRepository.deleteAllByAppointmentDateAppointmentAndMemberId(appointment, current.getId());

        Map<LocalDate, List<LocalTime>> grouped = new HashMap<>();
        for (AppointmentTimeRegisterRequestDto.TimeSlot slot : request.timeSlots()) {
            if (slot == null || slot.date() == null || slot.time() == null) {
                continue;
            }
            if (!dateMap.containsKey(slot.date())) {
                throw new BusinessException(ErrorCode.APPOINTMENT_DATE_MISMATCH);
            }
            if (isOutOfRange(slot.time(), appointment.getStartTime(), appointment.getEndTime())) {
                throw new BusinessException(ErrorCode.APPOINTMENT_TIME_OUT_OF_RANGE);
            }
            grouped.computeIfAbsent(slot.date(), ignored -> new ArrayList<>()).add(slot.time());
        }
        if (grouped.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        for (Map.Entry<LocalDate, List<LocalTime>> entry : grouped.entrySet()) {
            AppointmentDate date = dateMap.get(entry.getKey());
            List<LocalTime> times = entry.getValue().stream().distinct().sorted().toList();
            for (AppointmentSlotCalculator.TimeRange range : slotCalculator.compressToRanges(times)) {
                availableTimeRepository.save(AvailableTime.of(current.getId(), date, range.start(), range.end()));
            }
        }
    }

    @Transactional
    public void confirmTeamAppointment(UUID teamId, UUID appointmentId, AppointmentConfirmRequestDto request) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        TeamMembers membership = requireMembership(teamId, current.getId());
        Appointment appointment = getAppointmentDetail(teamId, appointmentId);
        requireCanManageAppointment(membership, appointment, current.getId());

        if (request == null || request.date() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (appointment.getState() == AppointmentState.CONFIRMED
                || appointment.getState() == AppointmentState.COMPLETE
                || appointment.getState() == AppointmentState.CANCELED
                || appointment.getState() == AppointmentState.EXPIRED) {
            throw new BusinessException(ErrorCode.INVALID_APPOINTMENT_STATE);
        }

        Set<LocalDate> appointmentDates = appointment.getAppointmentDateList().stream()
                .map(AppointmentDate::getDate)
                .collect(Collectors.toSet());
        if (!appointmentDates.contains(request.date())) {
            throw new BusinessException(ErrorCode.APPOINTMENT_DATE_MISMATCH);
        }

        LocalTime confirmedStart = appointment.isOnlyDate() ? LocalTime.MIN : request.startTime();
        LocalTime confirmedEnd = appointment.isOnlyDate() ? LocalTime.of(23, 59) : request.endTime();
        if (!appointment.isOnlyDate()) {
            if (request.startTime() == null || request.endTime() == null || !request.endTime().isAfter(request.startTime())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            if (request.startTime().isBefore(appointment.getStartTime()) || request.endTime().isAfter(appointment.getEndTime())) {
                throw new BusinessException(ErrorCode.APPOINTMENT_TIME_OUT_OF_RANGE);
            }
        }

        appointment.confirm(request.date(), confirmedStart, confirmedEnd);
        appointment.updateState(AppointmentState.CONFIRMED);
    }

    @Transactional
    public void deleteTeamAppointment(UUID teamId, UUID appointmentId) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        TeamMembers membership = requireMembership(teamId, current.getId());
        Appointment appointment = getAppointment(teamId, appointmentId);
        requireCanManageAppointment(membership, appointment, current.getId());
        appointmentRepository.delete(appointment);
    }

    @Transactional
    public void updateTeamAppointment(UUID teamId, UUID appointmentId, AppointmentUpdateRequestDto request) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        TeamMembers membership = requireMembership(teamId, current.getId());
        Appointment appointment = getAppointmentDetail(teamId, appointmentId);
        requireCanManageAppointment(membership, appointment, current.getId());

        appointmentCommandPolicy.validateAppointmentCommand(
                request.name(), request.appointmentDates(), request.onlyDate(), request.startTime(), request.endTime()
        );
        availableTimeRepository.deleteAllByAppointmentDateAppointment(appointment);
        appointmentDateRepository.deleteAllByAppointment(appointment);
        appointment.reschedule(
                request.name(),
                request.description(),
                request.onlyDate(),
                request.startTime(),
                request.endTime(),
                request.deadlineDateTime(),
                request.appointmentDates()
        );
    }

    private Member getCurrentMemberWithProfile() {
        Member current = currentMemberProvider.getCurrentMember();
        if (!current.isProfileSetupCompleted()) {
            throw new BusinessException(ErrorCode.PROFILE_SETUP_REQUIRED);
        }
        return current;
    }

    private void ensureTeamExists(UUID teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
    }

    private TeamMembers requireMembership(UUID teamId, Long memberId) {
        return teamMembersRepository.findByTeamIdAndMemberId(teamId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM));
    }

    private Appointment getAppointment(UUID teamId, UUID appointmentId) {
        return appointmentRepository.findByIdAndTeamId(appointmentId, teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    private Appointment getAppointmentDetail(UUID teamId, UUID appointmentId) {
        return appointmentRepository.findDetailByIdAndTeamId(appointmentId, teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    private void requireCanManageAppointment(TeamMembers membership, Appointment appointment, Long currentMemberId) {
        boolean isAdmin = membership.getTeamRole() == TeamRole.ADMIN;
        boolean isCreator = appointment.isCreatedBy(currentMemberId);
        if (!isAdmin && !isCreator) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean isOutOfRange(LocalTime time, LocalTime startTime, LocalTime endTime) {
        return time.isBefore(startTime) || !time.isBefore(endTime);
    }

    private String resolveOrganizerName(Appointment appointment, Map<Long, Member> memberMap, List<TeamMembers> memberships) {
        Member creator = memberMap.get(appointment.getCreatedByMemberId());
        if (creator == null) {
            TeamMembers leader = memberships.stream()
                    .filter(m -> m.getTeamRole() == TeamRole.ADMIN)
                    .findFirst()
                    .orElse(null);
            if (leader == null) {
                return null;
            }
            Member leaderMember = memberMap.get(leader.getMemberId());
            return leaderMember == null ? null : (leaderMember.getNickName() != null ? leaderMember.getNickName() : leaderMember.getName());
        }
        return creator.getNickName() != null ? creator.getNickName() : creator.getName();
    }

    private MemberInfo toMemberInfo(TeamMembers membership, Member member) {
        if (member == null) {
            return MemberInfo.of(membership.getMemberId(), null, null);
        }
        String displayName = membership.getTeamNickName() != null ? membership.getTeamNickName() : member.getNickName();
        if (displayName == null) {
            displayName = member.getName();
        }
        String profile = membership.getTeamProfileImage() != null ? membership.getTeamProfileImage() : member.getProfileImage();
        return MemberInfo.of(member.getId(), displayName, profile);
    }

    private List<AppointmentSlotCalculator.SlotAggregate> aggregateAvailableSlots(Appointment appointment) {
        List<AvailableTime> times = availableTimeRepository.findAllByAppointmentDateAppointment(appointment);
        List<Long> memberIds = times.stream().map(AvailableTime::getMemberId).distinct().toList();
        Map<Long, Member> memberMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, member -> member));
        return slotCalculator.aggregateSlots(times, memberMap);
    }

    private AppointmentListResponseDto.AvailableDateTime toAvailableDateTime(AppointmentSlotCalculator.SlotAggregate aggregate) {
        return new AppointmentListResponseDto.AvailableDateTime(
                aggregate.date(),
                aggregate.startTime(),
                aggregate.endTime(),
                (long) aggregate.memberIds().size(),
                new ArrayList<>(aggregate.memberNames())
        );
    }

    private AppointmentTimeTableResponseDto.AvailableDateTimeSlot toTimeTableSlot(AppointmentSlotCalculator.SlotAggregate aggregate) {
        return new AppointmentTimeTableResponseDto.AvailableDateTimeSlot(aggregate.date(), aggregate.startTime());
    }
}
