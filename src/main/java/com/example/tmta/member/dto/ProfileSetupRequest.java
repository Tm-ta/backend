package com.example.tmta.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "최초 프로필 설정 요청 DTO")
public record ProfileSetupRequest(
        @Schema(description = "닉네임", example = "백엔드몽")
        @NotBlank @Size(max = 30) String nickname,
        @Schema(description = "프로필 이미지 버킷", example = "tmta-prod-assets")
        @Size(max = 63) String profileImageBucket,
        @Schema(description = "프로필 이미지 오브젝트 키", example = "member-profile/1/20260227/uuid.png")
        @Size(max = 255) String profileImageKey
) {
    @AssertTrue(message = "profileImageBucket과 profileImageKey는 함께 전달해야 합니다.")
    public boolean isImagePairValid() {
        boolean bothNull = profileImageBucket == null && profileImageKey == null;
        boolean bothPresent = profileImageBucket != null && !profileImageBucket.isBlank()
                && profileImageKey != null && !profileImageKey.isBlank();
        return bothNull || bothPresent;
    }
}
