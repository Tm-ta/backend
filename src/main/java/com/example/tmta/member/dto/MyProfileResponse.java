package com.example.tmta.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보 조회 응답")
public record MyProfileResponse(
        @Schema(description = "회원 ID", example = "1")
        Long id,
        @Schema(description = "이메일", example = "user@example.com")
        String email,
        @Schema(description = "닉네임", example = "tmta_user")
        String nickname,
        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png")
        String profileImage
) {
}
