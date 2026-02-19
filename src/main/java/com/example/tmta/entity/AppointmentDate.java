package com.example.tmta.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentDate extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @OneToMany(mappedBy = "appointmentDate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvailableTime> availableTimeList = new ArrayList<>();

    @Builder
    private AppointmentDate(LocalDate date, Appointment appointment) {
        this.date = date;
        this.appointment = appointment;
    }

    public static AppointmentDate of(Appointment appointment, LocalDate date) {
        return AppointmentDate.builder()
                .appointment(appointment)
                .date(date)
                .build();
    }
}
