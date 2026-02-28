package com.example.tmta.storage;

import com.example.tmta.common.exception.dto.ErrorResponse;
import com.example.tmta.common.security.UserPrincipal;
import com.example.tmta.storage.dto.PresignUploadRequest;
import com.example.tmta.storage.dto.PresignUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Storage", description = "파일 업로드 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/storage")
public class S3PresignController {

    private final S3PresignService s3PresignService;

    @Operation(
            summary = "S3 업로드 pre-signed URL 발급",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- 응답의 `bucket`, `key`를 DB에 저장하고, 파일 업로드에는 `presignedUrl`을 사용합니다.
- 발급 URL은 만료 시간이 지나면 사용할 수 없습니다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PresignUploadRequest.class),
                            examples = @ExampleObject(
                                    name = "request",
                                    value = """
                                            {
                                              "purpose": "MEMBER_PROFILE",
                                              "fileName": "profile.png",
                                              "contentType": "image/png"
                                            }
                                            """
                            )
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PresignUploadResponse.class),
                            examples = @ExampleObject(
                                    name = "response",
                                    value = """
                                            {
                                              "bucket": "tmta-prod-assets",
                                              "key": "member-profile/1/20260228/123e4567-e89b-12d3-a456-426614174000.png",
                                              "presignedUrl": "https://tmta-prod-assets.s3.ap-northeast-2.amazonaws.com/member-profile/1/20260228/123e4567-e89b-12d3-a456-426614174000.png?...",
                                              "method": "PUT",
                                              "expiresInSeconds": 300
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "입력값 오류 (C001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (C003)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/presign/upload")
    public ResponseEntity<PresignUploadResponse> createUploadPresignedUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PresignUploadRequest request
    ) {
        return ResponseEntity.ok(s3PresignService.createUploadUrl(principal.memberId(), request));
    }
}
