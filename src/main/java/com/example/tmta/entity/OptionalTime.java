package com.example.tmta.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

import static jakarta.persistence.GenerationType.*;

@Entity
@Getter
public class OptionalTime {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer voteCount;
}
