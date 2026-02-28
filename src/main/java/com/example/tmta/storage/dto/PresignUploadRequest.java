package com.example.tmta.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "S3 업로드용 pre-signed URL 발급 요청")
public record PresignUploadRequest(
        @NotNull
        @Schema(description = "업로드 용도", example = "MEMBER_PROFILE")
        UploadPurpose purpose,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "원본 파일명", example = "profile.png")
        String fileName,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "MIME 타입", example = "image/png")
        String contentType
) {
    public enum UploadPurpose {
        MEMBER_PROFILE("member-profile"),
        TEAM_PROFILE("team-profile");

        private final String keyPrefix;

        UploadPurpose(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public String keyPrefix() {
            return keyPrefix;
        }
    }
}
