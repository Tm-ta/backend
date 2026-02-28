package com.example.tmta.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "멤버 정보 DTO")
public record MemberInfo(
		@Schema(description = "이름", example = "홍길동")
		String name,
		@Schema(description = "멤버 ID", example = "1")
		String id,
		@Schema(description = "프로필 이미지 상세 경로(bucket/key)", example = "tmta-prod-assets/member-profile/1/20260227/uuid.png")
		String profileImagePath
) {
	public static MemberInfo of(Long memberId, String displayName, String profileImagePath) {
		return new MemberInfo(displayName, String.valueOf(memberId), profileImagePath);
	}
}
