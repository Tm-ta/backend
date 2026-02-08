package com.example.tmta.dto.team;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "팀 가입 요청 DTO")
public class RequestJoinDto {
    @Schema(description = "사용자 이름", example = "testuser")
    private String userName;
    @Schema(description = "프로필 이미지 URL", example = "http://example.com/profile.jpg")
    private String profileImg;
}
