package com.example.tmta.service;

import com.example.tmta.dto.appointment.AppointmentCandidateFilter;
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
import com.example.tmta.entity.Team;
import com.example.tmta.entity.TeamMembers;
import com.example.tmta.entity.type.AppointmentState;
import com.example.tmta.entity.type.TeamRole;
import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import com.example.tmta.repository.AppointmentDateRepository;
import com.example.tmta.repository.AppointmentRepository;
import com.example.tmta.repository.AvailableTimeRepository;
import com.example.tmta.repository.TeamMembersRepository;
import com.example.tmta.repository.TeamRepository;
import com.example.tmta.security.CurrentMemberProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final CurrentMemberProvider currentMemberProvider;

    @Transactional
    public UUID createTeamAppointment(UUID teamId, AppointmentSaveRequestDto request) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = getTeam(teamId);
        requireTeamMember(team, member);

        List<LocalDate> appointmentDates = normalizeDates(request.getAppointmentDates());
        validateName(request.getName());

        Appointment appointment = Appointment.builder()
                .team(team)
                .createdBy(member)
                .name(request.getName().trim())
                .description(request.getDescription())
                .address(null)
                .startTime(resolveStartTime(request.isOnlyDate(), request.getStartTime()))
                .endTime(resolveEndTime(request.isOnlyDate(), request.getEndTime()))
                .state(AppointmentState.SCHEDULING)
                .build();

        appointmentDates.forEach(date -> appointment.addAppointmentDate(AppointmentDate.of(appointment, date)));

        Appointment saved = appointmentRepository.save(appointment);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public AppointmentDetailResponseDto getTeamAppointmentDetail(UUID teamId, UUID appointmentId) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = getTeam(teamId);
        requireTeamMember(team, member);
        Appointment appointment = getAppointment(teamId, appointmentId);

        AppointmentDetailResponseDto response = new AppointmentDetailResponseDto();
        response.setName(appointment.getName());
        response.setDescription(appointment.getDescription());
        response.setState(appointment.getState());
        response.setMemberCount((long) teamMembersRepository.findAllByTeam(team).size());
        response.setOrganizerName(resolveOrganizerName(team, appointment));

        List<LocalDate> dates = appointment.getAppointmentDateList().stream()
                .map(AppointmentDate::getDate)
                .sorted()
                .toList();
        if (!dates.isEmpty()) {
            response.setStartDate(dates.get(0));
            response.setEndDate(dates.get(dates.size() - 1));
        }

        List<String> memberNames = teamMembersRepository.findAllByTeam(team).stream()
                .map(tm -> tm.getTeamNickName() != null ? tm.getTeamNickName() : tm.getMember().getNickName())
                .filter(Objects::nonNull)
                .toList();
        response.setMember(memberNames);

        return response;
    }

    @Transactional(readOnly = true)
    public AppointmentListResponseDto getTeamAppointmentCandidate(UUID teamId, UUID appointmentId, AppointmentCandidateFilter filter) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = getTeam(teamId);
        requireTeamMember(team, member);
        Appointment appointment = getAppointment(teamId, appointmentId);

        List<SlotAggregate> aggregates = aggregateSlots(appointment);
        List<SlotAggregate> filtered = applyFilter(aggregates, filter);

        AppointmentListResponseDto response = new AppointmentListResponseDto();
        response.setAppointmentId(appointmentId);
        response.setAvailableSlots(toAvailableSlots(filtered));
        return response;
    }

    @Transactional(readOnly = true)
    public AppointmentTimeTableResponseDto getTeamAppointmentTimeTable(UUID teamId, UUID appointmentId, AppointmentCandidateFilter filter) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = getTeam(teamId);
        requireTeamMember(team, member);
        Appointment appointment = getAppointment(teamId, appointmentId);

        List<SlotAggregate> aggregates = applyFilter(aggregateSlots(appointment), filter);

        AppointmentTimeTableResponseDto response = new AppointmentTimeTableResponseDto();
        response.setAppointmentId(appointmentId);
        response.setAvailableDateTimeSlots(toTimeTableSlots(aggregates));
        return response;
    }

    @Transactional
    public void deadlineTeamAppointment(UUID teamId, UUID appointmentId) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = getTeam(teamId);
        TeamMembers me = requireTeamMember(team, member);
        Appointment appointment = getAppointment(teamId, appointmentId);
        requireCanManageAppointment(me, appointment, member);
        appointment.updateState(AppointmentState.SCHEDULING_CLOSED);
    }

    @Transactional
    public void registerTeamAppointmentTime(UUID teamId, UUID appointmentId, AppointmentTimeRegisterRequestDto request) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = getTeam(teamId);
        requireTeamMember(team, member);
        Appointment appointment = getAppointment(teamId, appointmentId);

        if (request == null || request.getTimeSlots() == null || request.getTimeSlots().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Map<LocalDate, AppointmentDate> appointmentDateByDate = appointment.getAppointmentDateList().stream()
                .collect(Collectors.toMap(AppointmentDate::getDate, d -> d));

        List<AppointmentTimeRegisterRequestDto.TimeSlot> validSlots = request.getTimeSlots().stream()
                .filter(slot -> slot != null && slot.getDate() != null && slot.getTime() != null)
                .toList();
        if (validSlots.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        availableTimeRepository.deleteAllByAppointmentDateAppointmentAndMember(appointment, member);

        Map<LocalDate, List<LocalTime>> groupedByDate = validSlots.stream()
                .collect(Collectors.groupingBy(
                        AppointmentTimeRegisterRequestDto.TimeSlot::getDate,
                        Collectors.mapping(AppointmentTimeRegisterRequestDto.TimeSlot::getTime, Collectors.toList())
                ));

        for (Map.Entry<LocalDate, List<LocalTime>> entry : groupedByDate.entrySet()) {
            AppointmentDate appointmentDate = appointmentDateByDate.get(entry.getKey());
            if (appointmentDate == null) {
                continue;
            }

            List<LocalTime> times = entry.getValue().stream().distinct().sorted().toList();
            for (TimeRange range : compressToRanges(times)) {
                availableTimeRepository.save(AvailableTime.of(member, appointmentDate, range.start(), range.end()));
            }
        }
    }

    @Transactional
    public void deleteTeamAppointment(UUID teamId, UUID appointmentId) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = getTeam(teamId);
        TeamMembers me = requireTeamMember(team, member);
        Appointment appointment = getAppointment(teamId, appointmentId);
        requireCanManageAppointment(me, appointment, member);
        appointmentRepository.delete(appointment);
    }

    @Transactional
    public void updateTeamAppointment(UUID teamId, UUID appointmentId, AppointmentUpdateRequestDto request) {
        Member member = currentMemberProvider.getCurrentMember();
        requireProfileSetupCompleted(member);

        Team team = getTeam(teamId);
        TeamMembers me = requireTeamMember(team, member);
        Appointment appointment = getAppointment(teamId, appointmentId);
        requireCanManageAppointment(me, appointment, member);
        validateName(request.getName());

        List<LocalDate> appointmentDates = normalizeDates(request.getAppointmentDates());
        appointment.updateBasics(
                request.getName().trim(),
                request.getDescription(),
                resolveStartTime(request.isOnlyDate(), request.getStartTime()),
                resolveEndTime(request.isOnlyDate(), request.getEndTime())
        );

        availableTimeRepository.deleteAllByAppointmentDateAppointment(appointment);
        appointmentDateRepository.deleteAllByAppointment(appointment);
        appointment.clearAppointmentDates();
        appointmentDates.forEach(date -> appointment.addAppointmentDate(AppointmentDate.of(appointment, date)));
    }

    private Team getTeam(UUID teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
    }

    private Appointment getAppointment(UUID teamId, UUID appointmentId) {
        return appointmentRepository.findByIdAndTeamId(appointmentId, teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    private TeamMembers requireTeamMember(Team team, Member member) {
        return teamMembersRepository.findByTeamAndMember(team, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_A_MEMBER_OF_TEAM));
    }

    private void requireProfileSetupCompleted(Member member) {
        if (!member.isProfileSetupCompleted()) {
            throw new BusinessException(ErrorCode.PROFILE_SETUP_REQUIRED);
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private List<LocalDate> normalizeDates(List<LocalDate> dates) {
        if (dates == null || dates.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return dates.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    private LocalTime resolveStartTime(boolean onlyDate, LocalTime requestStartTime) {
        return onlyDate ? LocalTime.MIN : (requestStartTime == null ? LocalTime.MIN : requestStartTime);
    }

    private LocalTime resolveEndTime(boolean onlyDate, LocalTime requestEndTime) {
        return onlyDate ? LocalTime.of(23, 59) : (requestEndTime == null ? LocalTime.of(23, 59) : requestEndTime);
    }

    private void requireCanManageAppointment(TeamMembers me, Appointment appointment, Member member) {
        boolean isAdmin = me.getTeamRole() == TeamRole.ADMIN;
        boolean isCreator = appointment.isCreatedBy(member.getId());
        if (!isAdmin && !isCreator) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private String resolveOrganizerName(Team team, Appointment appointment) {
        Member creator = appointment.getCreatedBy();
        if (creator == null) {
            return resolveLeaderName(team);
        }

        return teamMembersRepository.findByTeamAndMember(team, creator)
                .map(tm -> tm.getTeamNickName() != null ? tm.getTeamNickName() : tm.getMember().getNickName())
                .orElseGet(() -> creator.getNickName() != null ? creator.getNickName() : creator.getName());
    }

    private String resolveLeaderName(Team team) {
        return teamMembersRepository.findAllByTeam(team).stream()
                .filter(tm -> tm.getTeamRole() == TeamRole.ADMIN)
                .findFirst()
                .map(tm -> tm.getTeamNickName() != null ? tm.getTeamNickName() : tm.getMember().getNickName())
                .orElse(null);
    }

    private List<SlotAggregate> aggregateSlots(Appointment appointment) {
        List<AvailableTime> availableTimes = availableTimeRepository.findAllByAppointmentDateAppointment(appointment);
        Map<SlotKey, SlotAggregate> map = new LinkedHashMap<>();

        for (AvailableTime availableTime : availableTimes) {
            SlotKey key = new SlotKey(availableTime.getAppointmentDate().getDate(), availableTime.getStartTime(), availableTime.getEndTime());
            SlotAggregate aggregate = map.computeIfAbsent(key, k -> new SlotAggregate(k.date(), k.start(), k.end()));
            Member availableMember = availableTime.getMember();
            aggregate.memberIds.add(availableMember.getId());
            String displayName = availableMember.getNickName() != null ? availableMember.getNickName() : availableMember.getName();
            if (displayName != null) {
                aggregate.memberNames.add(displayName);
            }
        }

        return map.values().stream()
                .sorted(Comparator.comparing(SlotAggregate::date).thenComparing(SlotAggregate::startTime))
                .toList();
    }

    private List<SlotAggregate> applyFilter(List<SlotAggregate> source, AppointmentCandidateFilter filter) {
        List<Long> userIds = filter == null ? null : filter.getUserIds();
        Long availableUserCount = filter == null ? null : filter.getAvailableUserCount();
        Long availableTimeSlotCount = filter == null ? null : filter.getAvailableTimeSlotCount();

        return source.stream()
                .filter(slot -> userIds == null || userIds.isEmpty() || slot.memberIds.containsAll(userIds))
                .filter(slot -> availableUserCount == null || slot.memberIds.size() >= availableUserCount)
                .filter(slot -> availableTimeSlotCount == null || availableTimeSlotCount <= 1
                        || durationSlotCount(slot.startTime(), slot.endTime()) >= availableTimeSlotCount)
                .toList();
    }

    private long durationSlotCount(LocalTime startTime, LocalTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            return 0;
        }
        return Math.max(1, minutes / 30);
    }

    private List<AppointmentListResponseDto.AvailableDateTime> toAvailableSlots(List<SlotAggregate> aggregates) {
        return aggregates.stream().map(slot -> {
            AppointmentListResponseDto.AvailableDateTime dto = new AppointmentListResponseDto.AvailableDateTime();
            dto.setDate(slot.date());
            dto.setStartTime(slot.startTime());
            dto.setEndTime(slot.endTime());
            dto.setAvailableUserCount((long) slot.memberIds.size());
            dto.setAvailableUserNames(new ArrayList<>(slot.memberNames));
            return dto;
        }).toList();
    }

    private List<AppointmentTimeTableResponseDto.AvailableDateTimeSlot> toTimeTableSlots(List<SlotAggregate> aggregates) {
        List<AppointmentTimeTableResponseDto.AvailableDateTimeSlot> response = new ArrayList<>();
        for (SlotAggregate slot : aggregates) {
            AppointmentTimeTableResponseDto.AvailableDateTimeSlot dto = new AppointmentTimeTableResponseDto.AvailableDateTimeSlot();
            dto.setDate(slot.date());
            dto.setTime(slot.startTime());
            response.add(dto);
        }
        return response;
    }

    private List<TimeRange> compressToRanges(List<LocalTime> sortedTimes) {
        if (sortedTimes.isEmpty()) {
            return List.of();
        }

        List<TimeRange> ranges = new ArrayList<>();
        LocalTime rangeStart = sortedTimes.get(0);
        LocalTime prev = sortedTimes.get(0);

        for (int i = 1; i < sortedTimes.size(); i++) {
            LocalTime cur = sortedTimes.get(i);
            if (!cur.equals(prev.plusMinutes(30))) {
                ranges.add(new TimeRange(rangeStart, prev.plusMinutes(30)));
                rangeStart = cur;
            }
            prev = cur;
        }
        ranges.add(new TimeRange(rangeStart, prev.plusMinutes(30)));
        return ranges;
    }

    private record SlotKey(LocalDate date, LocalTime start, LocalTime end) {
    }

    private static class SlotAggregate {
        private final LocalDate date;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final Set<Long> memberIds = new java.util.HashSet<>();
        private final Set<String> memberNames = new java.util.LinkedHashSet<>();

        private SlotAggregate(LocalDate date, LocalTime startTime, LocalTime endTime) {
            this.date = date;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDate date() {
            return date;
        }

        public LocalTime startTime() {
            return startTime;
        }

        public LocalTime endTime() {
            return endTime;
        }
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
