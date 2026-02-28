package com.example.tmta.team.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "팀 프로필 설정/수정 요청 DTO")
public record TeamProfileSetupRequestDto(
        @NotBlank
        @Size(max = 30)
        @Schema(description = "팀에서 사용할 닉네임", example = "백엔드몽")
        String teamNickName,
        @Size(max = 63)
        @Schema(description = "팀에서 사용할 프로필 이미지 버킷", example = "tmta-prod-assets")
        String teamProfileImageBucket,
        @Size(max = 255)
        @Schema(description = "팀에서 사용할 프로필 이미지 오브젝트 키", example = "team-profile/1/20260227/uuid.png")
        String teamProfileImageKey
) {
    @AssertTrue(message = "teamProfileImageBucket과 teamProfileImageKey는 함께 전달해야 합니다.")
    public boolean isImagePairValid() {
        boolean bothNull = teamProfileImageBucket == null && teamProfileImageKey == null;
        boolean bothPresent = teamProfileImageBucket != null && !teamProfileImageBucket.isBlank()
                && teamProfileImageKey != null && !teamProfileImageKey.isBlank();
        return bothNull || bothPresent;
    }
}
