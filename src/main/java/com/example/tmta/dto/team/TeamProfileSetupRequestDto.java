package com.example.tmta.dto.team;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "팀 프로필 설정/수정 요청 DTO")
public record TeamProfileSetupRequestDto(
        @NotBlank
        @Size(max = 30)
        @Schema(description = "팀에서 사용할 닉네임", example = "백엔드몽")
        String teamNickName,
        @Size(max = 255)
        @Schema(description = "팀에서 사용할 프로필 이미지 URL", example = "http://example.com/team-profile.jpg")
        String teamProfileImage
) {
}
