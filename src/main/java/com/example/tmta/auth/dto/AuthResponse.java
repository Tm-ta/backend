package com.example.tmta.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthResponse(
        @Schema(description = "JWT Access Token") String accessToken,
        @Schema(description = "Bearer") String tokenType,
        @Schema(description = "최초 프로필 설정 필요 여부") boolean needsProfileSetup
) {
}
