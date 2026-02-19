package com.example.tmta.dto.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Data;

@Data
public class AppointmentTimeRegisterRequestDto {
	private List<TimeSlot> timeSlots;

	@Data
	public static class TimeSlot {
		private LocalDate date; // "YYYY-MM-DD" 형식
		private LocalTime time; // "HH:mm" 형식
	}
}
