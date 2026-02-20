package com.example.tmta.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_appointment_setting_deadline", columnList = "deadline_date_time")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentSetting extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(nullable = false)
    private boolean onlyDate;

    @Column(name = "deadline_date_time")
    private LocalDateTime deadlineDateTime;

    @Version
    private Long version;

    @Builder
    private AppointmentSetting(Appointment appointment, boolean onlyDate, LocalDateTime deadlineDateTime) {
        this.appointment = appointment;
        this.onlyDate = onlyDate;
        this.deadlineDateTime = deadlineDateTime;
    }

    public static AppointmentSetting of(Appointment appointment, boolean onlyDate, LocalDateTime deadlineDateTime) {
        return AppointmentSetting.builder()
                .appointment(appointment)
                .onlyDate(onlyDate)
                .deadlineDateTime(deadlineDateTime)
                .build();
    }

    public void update(boolean onlyDate, LocalDateTime deadlineDateTime) {
        this.onlyDate = onlyDate;
        this.deadlineDateTime = deadlineDateTime;
    }
}
