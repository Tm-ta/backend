package com.example.tmta.auth.dto;

import com.example.tmta.member.entity.type.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "소셜 로그인 요청 DTO")
public record SocialLoginRequest(
        @Schema(description = "소셜 제공자", example = "GOOGLE")
        @NotNull AuthProvider provider,
        @Schema(description = "제공자 토큰 (Google/Apple: ID Token, Kakao/Naver: Access Token)")
        @NotBlank String providerToken,
        @Schema(description = "Apple 재로그인 등 이메일 미포함 케이스용 이메일 힌트", example = "user@example.com")
        String emailHint,
        @Schema(description = "마케팅 수신 동의 (신규 가입시에만 반영)", example = "false")
        Boolean marketingAgreed,
        @Schema(description = "클라이언트 플랫폼 식별값 (로깅/운영 추적용)", example = "ios")
        String clientPlatform
) {
}
