package com.example.tmta.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

import static jakarta.persistence.GenerationType.*;

@Entity
@Getter
public class FinalTime {
    @Id
    @GeneratedValue(strategy = IDENTITY)

    @OneToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    private Long id;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
}
