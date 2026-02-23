package com.example.tmta.member;

import com.example.tmta.member.dto.MyProfileResponse;
import com.example.tmta.member.dto.ProfileSetupRequest;
import com.example.tmta.common.exception.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @Operation(
            summary = "최초 프로필 설정",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- nickname은 필수이며 최대 30자, profileImage는 최대 255자입니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 요청 필드 검증 실패.
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `MEMBER_NOT_FOUND (M001, 404)` : 회원 조회 실패.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "프로필 설정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류 (C001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "회원 없음 (M001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/me/profile-setup")
    public ResponseEntity<Void> setupProfile(@Valid @RequestBody ProfileSetupRequest request) {
        memberService.setupProfile(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "내 정보 조회",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**

### 예외상황 / 에러코드
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `MEMBER_NOT_FOUND (M001, 404)` : 회원 조회 실패.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "내 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "회원 없음 (M001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<MyProfileResponse> getMyProfile() {
        return ResponseEntity.ok(memberService.getMyProfile());
    }
}
