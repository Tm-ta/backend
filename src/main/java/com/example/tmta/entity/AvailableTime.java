package com.example.tmta.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailableTime extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_date_id")
    private AppointmentDate appointmentDate;

    private LocalTime startTime;
    private LocalTime endTime;

    @Builder
    private AvailableTime(Member member, AppointmentDate appointmentDate, LocalTime startTime, LocalTime endTime) {
        this.member = member;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static AvailableTime of(Member member, AppointmentDate appointmentDate, LocalTime startTime, LocalTime endTime) {
        return AvailableTime.builder()
                .member(member)
                .appointmentDate(appointmentDate)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
}
