package com.example.tmta.repository;

import com.example.tmta.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Optional<Appointment> findByIdAndTeamId(UUID appointmentId, UUID teamId);
}
