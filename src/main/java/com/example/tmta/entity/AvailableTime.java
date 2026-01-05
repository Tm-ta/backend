package com.example.tmta.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Getter
public class AvailableTime extends BaseEntity{
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    private LocalTime startTime;
    private LocalTime endTime;
}
