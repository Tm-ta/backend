package com.example.tmta.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 인증번호 확인 응답 DTO")
public record EmailVerificationConfirmResponse(
        @Schema(description = "인증 성공 여부", example = "true")
        boolean verified,
        @Schema(description = "회원가입 시 제출할 이메일 인증 토큰")
        String verificationToken
) {
}
