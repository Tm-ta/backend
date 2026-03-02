package com.example.tmta.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "팀 대표 프로필 이미지 업로드 pre-signed URL 발급 요청 DTO")
public record TeamProfileImagePresignRequestDto(
        @NotBlank
        @Size(max = 255)
        @Schema(description = "원본 파일명", example = "team-logo.png")
        String fileName,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "MIME 타입", example = "image/png")
        String contentType
) {
}
