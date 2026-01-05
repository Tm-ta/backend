package com.example.tmta.entity;

import com.example.tmta.entity.type.AppointmentState;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.GenerationType.*;

@Entity
@Getter
public class Appointment extends BaseEntity{
    @Id
    @GeneratedValue(strategy = UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    @OneToMany(mappedBy = "appointment")
    private List<AppointmentDate> appointmentDateList = new ArrayList<>();

    @OneToOne(mappedBy = "appointment")
    private FinalTime finalTime;

    @OneToMany(mappedBy = "appointment")
    private List<OptionalTime> optionalTimeList = new ArrayList<>();

    @OneToOne(mappedBy = "appointment")
    private AppointmentSetting setting;

    private String name;
    private String description;
    private String address;
    @ColumnDefault("00:00:00")
    private LocalTime startTime;
    @ColumnDefault("24:00:00")
    private LocalTime endTime;
    @Enumerated(value = STRING)
    private AppointmentState state;
}
