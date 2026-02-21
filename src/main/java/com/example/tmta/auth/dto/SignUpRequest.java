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
                regexp = "^(?=\\S{8,}$)(?=.*[A-Za-z])(?=.*\\d).*$",
                message = "비밀번호는 공백 없이 8자 이상이며, 영문과 숫자를 모두 포함해야 합니다."
        )
        @Pattern(
                regexp = "^(?!.*([A-Za-z0-9])\\1\\1).*$",
                message = "동일한 문자를 사용할 수 없어요"
        ) String password,
        @Schema(description = "이메일 인증번호 확인 API에서 발급된 토큰")
        @NotBlank(message = "이메일 인증 토큰은 필수입니다.")
        String verificationToken
) {
}
