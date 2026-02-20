package com.example.tmta.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "약속 타임테이블 조회 응답 DTO")
public record AppointmentTimeTableResponseDto(
		@Schema(description = "약속 ID")
		UUID appointmentId,
		@Schema(description = "가능한 날짜/시간 슬롯 목록")
		List<AvailableDateTimeSlot> availableDateTimeSlots
) {
	@Schema(description = "가능 날짜/시간 슬롯")
	public record AvailableDateTimeSlot(
			@Schema(description = "날짜")
			LocalDate date,
			@Schema(description = "시간")
			LocalTime time
	) {
	}
}
