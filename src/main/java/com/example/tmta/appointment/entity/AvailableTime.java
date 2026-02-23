package com.example.tmta.appointment.entity;
import com.example.tmta.common.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_available_time_slot", columnNames = {"member_id", "appointment_date_id", "start_time", "end_time"})
        },
        indexes = {
                @Index(name = "idx_available_time_member", columnList = "member_id"),
                @Index(name = "idx_available_time_appointment_date", columnList = "appointment_date_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailableTime extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_date_id", nullable = false)
    private AppointmentDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Builder
    private AvailableTime(Long memberId, AppointmentDate appointmentDate, LocalTime startTime, LocalTime endTime) {
        this.memberId = memberId;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static AvailableTime of(Long memberId, AppointmentDate appointmentDate, LocalTime startTime, LocalTime endTime) {
        return AvailableTime.builder()
                .memberId(memberId)
                .appointmentDate(appointmentDate)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
}
