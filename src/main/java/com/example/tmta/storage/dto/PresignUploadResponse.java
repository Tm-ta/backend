package com.example.tmta.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "S3 업로드용 pre-signed URL 발급 응답")
public record PresignUploadResponse(
        @Schema(description = "대상 버킷", example = "tmta-prod-assets")
        String bucket,

        @Schema(description = "저장될 오브젝트 키", example = "member-profile/1/20260227/uuid.png")
        String key,

        @Schema(description = "PUT 업로드 URL", example = "https://tmta-prod-assets.s3.ap-northeast-2.amazonaws.com/member-profile/1/20260228/123e4567-e89b-12d3-a456-426614174000.png?...X-Amz-Signature=...")
        String presignedUrl,

        @Schema(description = "HTTP 메서드", example = "PUT")
        String method,

        @Schema(description = "URL 만료 시간(초)", example = "300")
        long expiresInSeconds
) {
}
