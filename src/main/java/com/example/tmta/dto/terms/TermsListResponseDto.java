package com.example.tmta.dto.terms;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "약관 목록 응답 DTO")
public record TermsListResponseDto(
        @Schema(description = "약관 목록")
        List<TermsSummary> terms
) {
    @Schema(description = "약관 요약 정보")
    public record TermsSummary(
            @Schema(description = "약관 코드", example = "SERVICE")
            String code,
            @Schema(description = "약관 제목", example = "서비스 이용약관")
            String title,
            @Schema(description = "필수 동의 여부", example = "true")
            boolean required,
            @Schema(description = "약관 버전", example = "v1.0")
            String version,
            @Schema(description = "시행일", example = "2026-02-20")
            LocalDate effectiveDate
    ) {
    }
}
