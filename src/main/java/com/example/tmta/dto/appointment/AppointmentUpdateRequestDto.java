package com.example.tmta.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "약속 수정 요청 DTO")
public record AppointmentUpdateRequestDto(
		@Schema(description = "약속 이름", example = "다음 주 회의")
		String name,
		@Schema(description = "가능 날짜 목록")
		List<LocalDate> appointmentDates,
		// 날짜만 선택하는 경우 startTime과 endTime은 null로 설정
		@Schema(description = "시작 시간 (onlyDate=false일 때 필수)", example = "09:00")
		LocalTime startTime,
		@Schema(description = "종료 시간 (onlyDate=false일 때 필수)", example = "18:00")
		LocalTime endTime,
		@Schema(description = "날짜만 선택 여부", example = "false")
		boolean onlyDate,
		@Schema(description = "약속 설명", example = "회의실 A에서 진행")
		String description,
		@Schema(description = "마감 일시", example = "2026-02-28T23:59:00")
		LocalDateTime deadlineDateTime
) {
}
