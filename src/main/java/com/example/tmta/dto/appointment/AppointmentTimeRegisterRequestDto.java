package com.example.tmta.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "약속 가능 시간 등록 요청 DTO")
public record AppointmentTimeRegisterRequestDto(
		@Schema(description = "가능 시간 슬롯 목록")
		List<TimeSlot> timeSlots
) {
	@Schema(description = "가능 시간 슬롯")
	public record TimeSlot(
			@Schema(description = "날짜", example = "2026-03-01")
			LocalDate date, // "YYYY-MM-DD" 형식
			@Schema(description = "시간", example = "14:00")
			LocalTime time // "HH:mm" 형식
	) {
	}
}
