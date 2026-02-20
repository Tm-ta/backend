package com.example.tmta.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "회원가입 요청 DTO")
public record SignUpRequest(
        @Schema(description = "이메일", example = "user@example.com")
        @Email @NotBlank String email,
        @Schema(description = "비밀번호", example = "P@ssw0rd!")
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=[\\]{};':\\\"\\\\|,.<>/?]).{8,}$",
                message = "비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다."
        )
        String password
) {
}
