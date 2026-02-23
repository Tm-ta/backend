package com.example.tmta.appointment;

import com.example.tmta.appointment.dto.AppointmentCandidateFilter;
import com.example.tmta.appointment.entity.Appointment;
import com.example.tmta.appointment.entity.AppointmentDate;
import com.example.tmta.appointment.entity.AvailableTime;
import com.example.tmta.member.entity.Member;
import com.example.tmta.member.entity.type.AuthProvider;
import com.example.tmta.member.entity.type.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentSlotCalculatorTest {

    private final AppointmentSlotCalculator calculator = new AppointmentSlotCalculator();

    @Test
    @DisplayName("aggregateSlots: 같은 날짜/시간대를 사용자 기준으로 집계한다")
    void aggregateSlotsGroupsByDateAndTimeRange() {
        LocalDate date = LocalDate.of(2026, 3, 1);
        AppointmentDate appointmentDate = AppointmentDate.of(Appointment.builder().build(), date);

        List<AvailableTime> times = List.of(
                AvailableTime.of(1L, appointmentDate, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                AvailableTime.of(2L, appointmentDate, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                AvailableTime.of(1L, appointmentDate, LocalTime.of(11, 0), LocalTime.of(11, 30))
        );

        Map<Long, Member> memberMap = Map.of(
                1L, member(1L, "nick1", "name1"),
                2L, member(2L, null, "name2")
        );

        List<AppointmentSlotCalculator.SlotAggregate> result = calculator.aggregateSlots(times, memberMap);

        assertThat(result).hasSize(2);
        AppointmentSlotCalculator.SlotAggregate first = result.get(0);
        assertThat(first.date()).isEqualTo(date);
        assertThat(first.startTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(first.endTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(first.memberIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(first.memberNames()).containsExactlyInAnyOrder("nick1", "name2");
    }

    @Test
    @DisplayName("applyFilter: userIds, 최소 인원, 최소 슬롯 길이 조건을 모두 적용한다")
    void applyFilterWithAllConditions() {
        LocalDate date = LocalDate.of(2026, 3, 1);
        AppointmentDate appointmentDate = AppointmentDate.of(Appointment.builder().build(), date);

        List<AvailableTime> times = List.of(
                AvailableTime.of(1L, appointmentDate, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                AvailableTime.of(2L, appointmentDate, LocalTime.of(10, 0), LocalTime.of(11, 0)),
                AvailableTime.of(1L, appointmentDate, LocalTime.of(11, 0), LocalTime.of(11, 30))
        );
        List<AppointmentSlotCalculator.SlotAggregate> aggregated = calculator.aggregateSlots(times, Map.of(
                1L, member(1L, "nick1", "name1"),
                2L, member(2L, "nick2", "name2")
        ));

        AppointmentCandidateFilter filter = new AppointmentCandidateFilter(
                List.of(1L, 2L),
                2L,
                2L
        );

        List<AppointmentSlotCalculator.SlotAggregate> filtered = calculator.applyFilter(aggregated, filter);

        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).startTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(filtered.get(0).endTime()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    @DisplayName("compressToRanges: 30분 연속 슬롯을 하나의 구간으로 압축한다")
    void compressToRanges() {
        List<LocalTime> sortedTimes = List.of(
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                LocalTime.of(11, 0),
                LocalTime.of(12, 0),
                LocalTime.of(12, 30)
        );

        List<AppointmentSlotCalculator.TimeRange> ranges = calculator.compressToRanges(sortedTimes);

        assertThat(ranges).hasSize(2);
        assertThat(ranges.get(0).start()).isEqualTo(LocalTime.of(10, 0));
        assertThat(ranges.get(0).end()).isEqualTo(LocalTime.of(11, 30));
        assertThat(ranges.get(1).start()).isEqualTo(LocalTime.of(12, 0));
        assertThat(ranges.get(1).end()).isEqualTo(LocalTime.of(13, 0));
    }

    private static Member member(Long id, String nickName, String name) {
        return Member.builder()
                .id(id)
                .nickName(nickName)
                .name(name)
                .email(id + "@test.com")
                .password("encoded")
                .authProvider(AuthProvider.LOCAL)
                .role(MemberRole.GENERAL)
                .build();
    }
}
