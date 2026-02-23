package com.example.tmta.appointment;

import com.example.tmta.appointment.dto.AppointmentCandidateFilter;
import com.example.tmta.appointment.entity.AvailableTime;
import com.example.tmta.member.entity.Member;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AppointmentSlotCalculator {

    /** 등록된 가능 시간 목록을 날짜/시간 슬롯 기준으로 집계합니다. */
    public List<SlotAggregate> aggregateSlots(List<AvailableTime> times, Map<Long, Member> memberMap) {
        Map<SlotKey, SlotAggregate> grouped = new HashMap<>();
        for (AvailableTime time : times) {
            SlotKey key = new SlotKey(time.getAppointmentDate().getDate(), time.getStartTime(), time.getEndTime());
            SlotAggregate aggregate = grouped.computeIfAbsent(key, k -> new SlotAggregate(k.date(), k.startTime(), k.endTime()));
            aggregate.memberIds.add(time.getMemberId());
        }

        for (SlotAggregate aggregate : grouped.values()) {
            for (Long memberId : aggregate.memberIds) {
                Member member = memberMap.get(memberId);
                if (member == null) {
                    continue;
                }
                String displayName = member.getNickName() != null ? member.getNickName() : member.getName();
                if (displayName != null) {
                    aggregate.memberNames.add(displayName);
                }
            }
        }

        return grouped.values().stream()
                .sorted(Comparator.comparing(SlotAggregate::date).thenComparing(SlotAggregate::startTime))
                .toList();
    }

    /** 사용자/인원/시간슬롯 조건으로 후보 슬롯을 필터링합니다. */
    public List<SlotAggregate> applyFilter(List<SlotAggregate> source, AppointmentCandidateFilter filter) {
        List<Long> userIds = filter == null ? null : filter.userIds();
        Long minUsers = filter == null ? null : filter.availableUserCount();
        Long minSlots = filter == null ? null : filter.availableTimeSlotCount();

        return source.stream()
                .filter(slot -> userIds == null || userIds.isEmpty() || slot.memberIds.containsAll(userIds))
                .filter(slot -> minUsers == null || slot.memberIds.size() >= minUsers)
                .filter(slot -> minSlots == null || minSlots <= 1 || durationSlotCount(slot.startTime(), slot.endTime()) >= minSlots)
                .toList();
    }

    /** 30분 단위 시간 목록을 연속 구간으로 압축합니다. */
    public List<TimeRange> compressToRanges(List<LocalTime> sortedTimes) {
        if (sortedTimes.isEmpty()) {
            return List.of();
        }
        List<TimeRange> ranges = new ArrayList<>();
        LocalTime start = sortedTimes.get(0);
        LocalTime previous = sortedTimes.get(0);
        for (int i = 1; i < sortedTimes.size(); i++) {
            LocalTime current = sortedTimes.get(i);
            if (!current.equals(previous.plusMinutes(30))) {
                ranges.add(new TimeRange(start, previous.plusMinutes(30)));
                start = current;
            }
            previous = current;
        }
        ranges.add(new TimeRange(start, previous.plusMinutes(30)));
        return ranges;
    }

    /** 시간 구간 길이를 30분 슬롯 개수로 계산합니다. */
    private long durationSlotCount(LocalTime start, LocalTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) {
            return 0;
        }
        return Math.max(1, minutes / 30);
    }

    /** 슬롯 그룹핑 키입니다. */
    private record SlotKey(LocalDate date, LocalTime startTime, LocalTime endTime) {
    }

    /** 후보 시간 슬롯 집계 결과입니다. */
    public static class SlotAggregate {
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

        public LocalDate date() {
            return date;
        }

        public LocalTime startTime() {
            return startTime;
        }

        public LocalTime endTime() {
            return endTime;
        }

        public Set<Long> memberIds() {
            return memberIds;
        }

        public Set<String> memberNames() {
            return memberNames;
        }
    }

    /** 연속 시간 구간(start/end) 표현입니다. */
    public record TimeRange(LocalTime start, LocalTime end) {
    }
}
