package com.example.tmta.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원가입 응답 DTO")
public record SignUpResponse(
        @Schema(description = "회원 ID", example = "1")
        Long memberId,
        @Schema(description = "이메일", example = "user@example.com")
        String email
) {
}
