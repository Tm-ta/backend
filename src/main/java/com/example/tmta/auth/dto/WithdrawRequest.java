package com.example.tmta.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 탈퇴 요청 DTO")
public record WithdrawRequest(
        @Schema(description = "LOCAL 계정인 경우 비밀번호 확인(권장)")
        String password
) {
}
