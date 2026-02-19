package com.example.tmta.repository;

import com.example.tmta.entity.Appointment;
import com.example.tmta.entity.AvailableTime;
import com.example.tmta.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailableTimeRepository extends JpaRepository<AvailableTime, Long> {
    List<AvailableTime> findAllByAppointmentDateAppointment(Appointment appointment);

    void deleteAllByAppointmentDateAppointmentAndMember(Appointment appointment, Member member);

    void deleteAllByAppointmentDateAppointment(Appointment appointment);
}
