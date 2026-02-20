package com.example.tmta.service;

import com.example.tmta.dto.MemberInfo;
import com.example.tmta.dto.appointment.AppointmentCandidateFilter;
import com.example.tmta.dto.appointment.AppointmentDetailResponseDto;
import com.example.tmta.dto.appointment.AppointmentListResponseDto;
import com.example.tmta.dto.appointment.AppointmentSaveRequestDto;
import com.example.tmta.dto.appointment.AppointmentTimeRegisterRequestDto;
import com.example.tmta.dto.appointment.AppointmentTimeTableResponseDto;
import com.example.tmta.dto.appointment.AppointmentUpdateRequestDto;
import com.example.tmta.entity.Appointment;
import com.example.tmta.entity.AppointmentDate;
import com.example.tmta.entity.AppointmentSetting;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.MIN;
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(23, 59);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentDateRepository appointmentDateRepository;
    private final AvailableTimeRepository availableTimeRepository;
    private final TeamRepository teamRepository;
    private final TeamMembersRepository teamMembersRepository;
    private final MemberRepository memberRepository;
    private final CurrentMemberProvider currentMemberProvider;

    @Transactional
    public UUID createTeamAppointment(UUID teamId, AppointmentSaveRequestDto request) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        requireMembership(teamId, current.getId());

        validateAppointmentCommand(request.name(), request.appointmentDates(), request.onlyDate(), request.startTime(), request.endTime());

        Appointment appointment = Appointment.builder()
                .teamId(teamId)
                .createdByMemberId(current.getId())
                .name(request.name().trim())
                .description(request.description())
                .startTime(resolveStartTime(request.onlyDate(), request.startTime()))
                .endTime(resolveEndTime(request.onlyDate(), request.endTime()))
                .state(AppointmentState.SCHEDULING)
                .build();
        appointment.assignSetting(AppointmentSetting.of(appointment, request.onlyDate(), request.deadlineDateTime()));
        toDistinctSortedDates(request.appointmentDates())
                .forEach(date -> appointment.addAppointmentDate(AppointmentDate.of(appointment, date)));

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

        List<SlotAggregate> filtered = applyCandidateFilter(aggregateAvailableSlots(appointment), filter);
        return new AppointmentListResponseDto(appointmentId, filtered.stream().map(this::toAvailableDateTime).toList());
    }

    @Transactional(readOnly = true)
    public AppointmentTimeTableResponseDto getTeamAppointmentTimeTable(UUID teamId, UUID appointmentId, AppointmentCandidateFilter filter) {
        Member current = getCurrentMemberWithProfile();
        ensureTeamExists(teamId);
        requireMembership(teamId, current.getId());
        Appointment appointment = getAppointmentDetail(teamId, appointmentId);

        List<SlotAggregate> filtered = applyCandidateFilter(aggregateAvailableSlots(appointment), filter);
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
        appointment.updateState(AppointmentState.SCHEDULING_CLOSED);
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
            for (TimeRange range : compressToRanges(times)) {
                availableTimeRepository.save(AvailableTime.of(current.getId(), date, range.start(), range.end()));
            }
        }
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

        validateAppointmentCommand(request.name(), request.appointmentDates(), request.onlyDate(), request.startTime(), request.endTime());
        appointment.updateBasics(
                request.name().trim(),
                request.description(),
                resolveStartTime(request.onlyDate(), request.startTime()),
                resolveEndTime(request.onlyDate(), request.endTime())
        );
        if (appointment.getSetting() == null) {
            appointment.assignSetting(AppointmentSetting.of(appointment, request.onlyDate(), request.deadlineDateTime()));
        } else {
            appointment.getSetting().update(request.onlyDate(), request.deadlineDateTime());
        }

        availableTimeRepository.deleteAllByAppointmentDateAppointment(appointment);
        appointmentDateRepository.deleteAllByAppointment(appointment);
        appointment.clearAppointmentDates();
        toDistinctSortedDates(request.appointmentDates())
                .forEach(date -> appointment.addAppointmentDate(AppointmentDate.of(appointment, date)));
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

    private void validateAppointmentCommand(String name, List<LocalDate> dates, boolean onlyDate, LocalTime startTime, LocalTime endTime) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (dates == null || dates.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!onlyDate && startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private List<LocalDate> toDistinctSortedDates(List<LocalDate> dates) {
        return dates.stream().filter(Objects::nonNull).distinct().sorted().toList();
    }

    private LocalTime resolveStartTime(boolean onlyDate, LocalTime time) {
        return onlyDate ? DEFAULT_START_TIME : (time == null ? DEFAULT_START_TIME : time);
    }

    private LocalTime resolveEndTime(boolean onlyDate, LocalTime time) {
        return onlyDate ? DEFAULT_END_TIME : (time == null ? DEFAULT_END_TIME : time);
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

    private List<SlotAggregate> aggregateAvailableSlots(Appointment appointment) {
        List<AvailableTime> times = availableTimeRepository.findAllByAppointmentDateAppointment(appointment);
        Map<SlotKey, SlotAggregate> grouped = new HashMap<>();
        for (AvailableTime time : times) {
            SlotKey key = new SlotKey(time.getAppointmentDate().getDate(), time.getStartTime(), time.getEndTime());
            SlotAggregate aggregate = grouped.computeIfAbsent(key, k -> new SlotAggregate(k.date(), k.startTime(), k.endTime()));
            aggregate.memberIds.add(time.getMemberId());
        }

        Map<Long, Member> memberMap = memberRepository.findAllById(
                grouped.values().stream().flatMap(s -> s.memberIds.stream()).distinct().toList()
        ).stream().collect(Collectors.toMap(Member::getId, member -> member));

        for (SlotAggregate aggregate : grouped.values()) {
            for (Long memberId : aggregate.memberIds) {
                Member member = memberMap.get(memberId);
                if (member == null) {
                    continue;
                }
                String name = member.getNickName() != null ? member.getNickName() : member.getName();
                if (name != null) {
                    aggregate.memberNames.add(name);
                }
            }
        }

        return grouped.values().stream()
                .sorted(Comparator.comparing(SlotAggregate::date).thenComparing(SlotAggregate::startTime))
                .toList();
    }

    private List<SlotAggregate> applyCandidateFilter(List<SlotAggregate> source, AppointmentCandidateFilter filter) {
        List<Long> userIds = filter == null ? null : filter.userIds();
        Long minUsers = filter == null ? null : filter.availableUserCount();
        Long minSlots = filter == null ? null : filter.availableTimeSlotCount();

        return source.stream()
                .filter(slot -> userIds == null || userIds.isEmpty() || slot.memberIds.containsAll(userIds))
                .filter(slot -> minUsers == null || slot.memberIds.size() >= minUsers)
                .filter(slot -> minSlots == null || minSlots <= 1 || durationSlotCount(slot.startTime(), slot.endTime()) >= minSlots)
                .toList();
    }

    private long durationSlotCount(LocalTime start, LocalTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) {
            return 0;
        }
        return Math.max(1, minutes / 30);
    }

    private AppointmentListResponseDto.AvailableDateTime toAvailableDateTime(SlotAggregate aggregate) {
        return new AppointmentListResponseDto.AvailableDateTime(
                aggregate.date(),
                aggregate.startTime(),
                aggregate.endTime(),
                (long) aggregate.memberIds.size(),
                new ArrayList<>(aggregate.memberNames)
        );
    }

    private AppointmentTimeTableResponseDto.AvailableDateTimeSlot toTimeTableSlot(SlotAggregate aggregate) {
        return new AppointmentTimeTableResponseDto.AvailableDateTimeSlot(aggregate.date(), aggregate.startTime());
    }

    private List<TimeRange> compressToRanges(List<LocalTime> sortedTimes) {
        if (sortedTimes.isEmpty()) {
            return List.of();
        }
        List<TimeRange> ranges = new ArrayList<>();
        LocalTime start = sortedTimes.get(0);
        LocalTime prev = sortedTimes.get(0);
        for (int i = 1; i < sortedTimes.size(); i++) {
            LocalTime cur = sortedTimes.get(i);
            if (!cur.equals(prev.plusMinutes(30))) {
                ranges.add(new TimeRange(start, prev.plusMinutes(30)));
                start = cur;
            }
            prev = cur;
        }
        ranges.add(new TimeRange(start, prev.plusMinutes(30)));
        return ranges;
    }

    private record SlotKey(LocalDate date, LocalTime startTime, LocalTime endTime) {}

    private static class SlotAggregate {
        private final LocalDate date;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final Set<Long> memberIds = new LinkedHashSet<>();
        private final Set<String> memberNames = new LinkedHashSet<>();

        private SlotAggregate(LocalDate date, LocalTime startTime, LocalTime endTime) {
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDate date() { return date; }
        public LocalTime startTime() { return startTime; }
        public LocalTime endTime() { return endTime; }
    }

    private record TimeRange(LocalTime start, LocalTime end) {}
}
