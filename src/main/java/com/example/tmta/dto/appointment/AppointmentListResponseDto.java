package com.example.tmta.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "약속 후보 시간 조회 응답 DTO")
public record AppointmentListResponseDto(
		@Schema(description = "약속 ID")
		UUID appointmentId,
		@Schema(description = "후보 시간 목록")
		List<AvailableDateTime> availableSlots
) {
	@Schema(description = "후보 시간 정보")
	public record AvailableDateTime(
			@Schema(description = "날짜")
			LocalDate date,
			// 날짜만 선택할 수 있기 때문에 시간은 00:00 ~ 23:59로 고정
			@Schema(description = "시작 시간")
			LocalTime startTime,
			@Schema(description = "종료 시간")
			LocalTime endTime,
			@Schema(description = "참여 가능 인원 수")
			Long availableUserCount,
			@Schema(description = "참여 가능 사용자 이름 목록")
			List<String> availableUserNames
	) {
	}
}
