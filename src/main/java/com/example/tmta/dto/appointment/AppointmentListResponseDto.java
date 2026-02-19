package com.example.tmta.dto.appointment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import lombok.Data;

@Data
public class AppointmentListResponseDto {
	private Long appointmentId;
	private List<AvailableDateTime> availableSlots;
	public static class AvailableDateTime {
		private LocalDate date;
		// 날짜만 선택할 수 있기 때문에 시간은 00:00 ~ 23:59로 고정
		private LocalTime startTime;
		private LocalTime endTime;
		private Long availableUserCount;
		private List<String> availableUserNames;
	}
}
