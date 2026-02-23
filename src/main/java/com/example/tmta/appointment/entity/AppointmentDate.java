package com.example.tmta.appointment.entity;
import com.example.tmta.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_appointment_date", columnNames = {"appointment_id", "date"})
        },
        indexes = {
                @Index(name = "idx_appointment_date_appointment", columnList = "appointment_id"),
                @Index(name = "idx_appointment_date_date", columnList = "date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentDate extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
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
