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
                regexp = "^(?=\\S{8,64}$)(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).*$",
                message = "비밀번호는 공백 없이 8~64자이며, 영문/숫자/특수문자를 각각 1개 이상 포함해야 합니다."
        ) String password,
        @Schema(description = "이메일 인증번호 확인 API에서 발급된 토큰")
        @NotBlank(message = "이메일 인증 토큰은 필수입니다.")
        String verificationToken
) {
}
