package com.example.tmta.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinalTime extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(nullable = false)
    private LocalDate date;
    @Column(nullable = false)
    private LocalTime startTime;
    @Column(nullable = false)
    private LocalTime endTime;

    @Builder
    private FinalTime(Appointment appointment, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.appointment = appointment;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static FinalTime of(Appointment appointment, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return FinalTime.builder()
                .appointment(appointment)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

    public void update(LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
