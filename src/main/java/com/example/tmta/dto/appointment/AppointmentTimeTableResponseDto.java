package com.example.tmta.dto.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class AppointmentTimeTableResponseDto {
	private UUID appointmentId;
	private List<AvailableDateTimeSlot> availableDateTimeSlots;
	@Data
	public static class AvailableDateTimeSlot{
		private LocalDate date;
		private LocalTime time;
	}
}
