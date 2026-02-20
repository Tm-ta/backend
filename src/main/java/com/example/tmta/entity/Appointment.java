package com.example.tmta.entity;

import com.example.tmta.entity.type.AppointmentState;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(indexes = {
        @Index(name = "idx_appointment_team", columnList = "team_id"),
        @Index(name = "idx_appointment_creator", columnList = "created_by_member_id"),
        @Index(name = "idx_appointment_state", columnList = "state")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "created_by_member_id", nullable = false)
    private Long createdByMemberId;

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

    @Column(nullable = false)
    private String name;
    private String description;
    private String address;
    @ColumnDefault("'00:00:00'")
    private LocalTime startTime;
    @ColumnDefault("'23:59:59'")
    private LocalTime endTime;
    @Enumerated(value = STRING)
    @Column(nullable = false)
    private AppointmentState state;

    @Version
    private Long version;

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
        return createdByMemberId != null && createdByMemberId.equals(memberId);
    }

    public void assignSetting(AppointmentSetting setting) {
        this.setting = setting;
    }

    public void confirm(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (this.finalTime == null) {
            this.finalTime = FinalTime.of(this, date, startTime, endTime);
            return;
        }
        this.finalTime.update(date, startTime, endTime);
    }

    public boolean isOnlyDate() {
        if (setting != null) {
            return setting.isOnlyDate();
        }
        return LocalTime.MIN.equals(startTime) && LocalTime.of(23, 59).equals(endTime);
    }

    public LocalDate getConfirmedDate() {
        return finalTime == null ? null : finalTime.getDate();
    }
}
