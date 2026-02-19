package com.example.tmta.dto.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Data;

@Data
public class AppointmentTimeTableResponseDto {
	private Long appointmentId;
	private List<AvailableDateTimeSlot> availableDateTimeSlots;
	public static class AvailableDateTimeSlot{
		private LocalDate date;
		private LocalTime time;
	}
}
