package com.example.tmta.appointment.repository;

import com.example.tmta.appointment.entity.Appointment;
import com.example.tmta.appointment.entity.AppointmentDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentDateRepository extends JpaRepository<AppointmentDate, Long> {
    void deleteAllByAppointment(Appointment appointment);
}
