package com.example.tmta.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일 인증번호 확인 요청 DTO")
public record EmailVerificationConfirmRequest(
        @Schema(description = "인증 대상 이메일", example = "user@example.com")
        @Email(message = "유효한 이메일 형식이어야 합니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,
        @Schema(description = "인증번호", example = "1111")
        @NotBlank(message = "인증번호는 필수입니다.")
        String code
) {
}
