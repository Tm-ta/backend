package com.example.tmta.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "최초 프로필 설정 요청 DTO")
public record ProfileSetupRequest(
        @Schema(description = "닉네임", example = "백엔드몽")
        @NotBlank @Size(max = 30) String nickname,
        @Schema(description = "프로필 이미지 URL", example = "http://example.com/profile.jpg")
        @Size(max = 255) String profileImage
) {
}
