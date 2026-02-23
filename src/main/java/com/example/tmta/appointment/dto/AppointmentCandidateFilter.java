package com.example.tmta.appointment.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "약속 후보 시간 필터")
public record AppointmentCandidateFilter(
		@Parameter(description = "지정한 사용자 ID 목록으로 필터링")
		@Schema(description = "사용자 ID 목록")
		List<Long> userIds,
		@Parameter(description = "해당 인원 수 이상 참여 가능한 시간대만 조회")
		@Schema(description = "최소 참여 가능 인원", example = "3")
		Long availableUserCount,
		@Parameter(description = "조회할 후보 시간 개수 제한")
		@Schema(description = "후보 시간 개수 제한", example = "10")
		Long availableTimeSlotCount
) {
}
