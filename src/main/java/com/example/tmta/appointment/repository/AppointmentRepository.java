package com.example.tmta.appointment.repository;

import com.example.tmta.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Optional<Appointment> findByIdAndTeamId(UUID appointmentId, UUID teamId);

    @EntityGraph(attributePaths = {
            "appointmentDateList",
            "setting",
            "finalTime"
    })
    Optional<Appointment> findDetailByIdAndTeamId(UUID appointmentId, UUID teamId);

    List<Appointment> findAllByTeamId(UUID teamId);
    List<Appointment> findAllByTeamIdIn(List<UUID> teamIds);
}
