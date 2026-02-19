package com.example.tmta.entity;

import com.example.tmta.entity.type.AppointmentState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_member_id")
    private Member createdBy;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AppointmentDate> appointmentDateList = new ArrayList<>();

    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private FinalTime finalTime;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OptionalTime> optionalTimeList = new ArrayList<>();

    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private AppointmentSetting setting;

    private String name;
    private String description;
    private String address;
    @ColumnDefault("'00:00:00'")
    private LocalTime startTime;
    @ColumnDefault("'23:59:59'")
    private LocalTime endTime;
    @Enumerated(value = STRING)
    private AppointmentState state;

    public void addAppointmentDate(AppointmentDate appointmentDate) {
        this.appointmentDateList.add(appointmentDate);
    }

    public void clearAppointmentDates() {
        this.appointmentDateList.clear();
    }

    public void updateBasics(String name, String description, LocalTime startTime, LocalTime endTime) {
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void updateState(AppointmentState state) {
        this.state = state;
    }

    public boolean isCreatedBy(Long memberId) {
        return createdBy != null && createdBy.getId() != null && createdBy.getId().equals(memberId);
    }
}
