package com.example.tmta.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "멤버 정보 DTO")
public record MemberInfo(
		@Schema(description = "이름", example = "홍길동")
		String name,
		@Schema(description = "멤버 ID", example = "1")
		String id,
		@Schema(description = "프로필 이미지 URL", example = "http://example.com/profile.jpg")
		String profileImageUrl
) {
	public static MemberInfo of(Long memberId, String displayName, String profileImageUrl) {
		return new MemberInfo(displayName, String.valueOf(memberId), profileImageUrl);
	}
}
