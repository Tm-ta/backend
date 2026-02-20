package com.example.tmta.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 DTO")
public record LoginRequest(
        @Schema(description = "이메일", example = "user@example.com")
        @Email @NotBlank String email,
        @Schema(description = "비밀번호", example = "P@ssw0rd!")
        @NotBlank String password
) {
}
