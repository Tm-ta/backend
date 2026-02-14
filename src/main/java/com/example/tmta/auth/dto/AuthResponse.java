package com.example.tmta.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    @Schema(description = "JWT Access Token")
    private String accessToken;

    @Schema(description = "Bearer")
    private String tokenType;

    @Schema(description = "최초 프로필 설정 필요 여부")
    private boolean needsProfileSetup;
}
