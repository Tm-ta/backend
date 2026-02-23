package com.example.tmta.appointment.dto;

import java.time.LocalDate;
import java.util.List;

import com.example.tmta.common.dto.MemberInfo;
import com.example.tmta.appointment.entity.type.AppointmentState;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약속 상세 조회 응답 DTO")
public record AppointmentDetailResponseDto(
		@Schema(description = "약속 이름")
		String name,
		@Schema(description = "약속 생성자 이름")
		String organizerName,
		@Schema(description = "팀 전체 인원 수")
		Long memberCount,
		@Schema(description = "시작일")
		LocalDate startDate,
		@Schema(description = "종료일")
		LocalDate endDate,
		@Schema(description = "팀 멤버 목록")
		List<MemberInfo> member,
		@Schema(description = "약속 설명")
		String description,
		@Schema(description = "약속 상태")
		AppointmentState state,
		@Schema(description = "확정일 (확정 전이면 null)")
		LocalDate confirmedDate
) {
}
