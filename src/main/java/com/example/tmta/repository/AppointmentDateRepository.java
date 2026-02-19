package com.example.tmta.repository;

import com.example.tmta.entity.Appointment;
import com.example.tmta.entity.AppointmentDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentDateRepository extends JpaRepository<AppointmentDate, Long> {
    void deleteAllByAppointment(Appointment appointment);
}
