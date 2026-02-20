package com.example.tmta.dto.appointment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "약속 확정 요청 DTO")
public record AppointmentConfirmRequestDto(
        @Schema(description = "확정 날짜", example = "2026-03-01")
        @NotNull
        LocalDate date,
        @Schema(description = "확정 시작 시간 (onlyDate=false일 때 필수)", example = "14:00")
        LocalTime startTime,
        @Schema(description = "확정 종료 시간 (onlyDate=false일 때 필수)", example = "15:00")
        LocalTime endTime
) {
}
