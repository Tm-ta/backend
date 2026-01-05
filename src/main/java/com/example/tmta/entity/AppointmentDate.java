package com.example.tmta.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

import static jakarta.persistence.GenerationType.*;

@Entity
@Getter
public class AppointmentDate extends BaseEntity{
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;
}
