package com.example.tmta.appointment.repository;

import com.example.tmta.appointment.entity.Appointment;
import com.example.tmta.appointment.entity.AvailableTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailableTimeRepository extends JpaRepository<AvailableTime, Long> {
    List<AvailableTime> findAllByAppointmentDateAppointment(Appointment appointment);

    void deleteAllByAppointmentDateAppointmentAndMemberId(Appointment appointment, Long memberId);

    void deleteAllByAppointmentDateAppointment(Appointment appointment);

    void deleteAllByMemberId(Long memberId);
}
