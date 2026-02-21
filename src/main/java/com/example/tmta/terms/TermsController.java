package com.example.tmta.terms;

import com.example.tmta.terms.dto.TermsDetailResponseDto;
import com.example.tmta.terms.dto.TermsListResponseDto;
import com.example.tmta.terms.TermsService;
import com.example.tmta.common.exception.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Terms", description = "약관 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/terms")
public class TermsController {

    private final TermsService termsService;

    @Operation(
            summary = "약관 목록 조회",
            description = """
### 제약조건
- 인증 없이 조회 가능합니다.

### 예외상황 / 에러코드
- (현재 비즈니스 예외 없음)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<TermsListResponseDto> getTermsList() {
        return ResponseEntity.ok(termsService.getTermsList());
    }

    @Operation(
            summary = "약관 상세 조회",
            description = """
### 제약조건
- code는 null/blank일 수 없습니다.
- code는 대소문자 구분 없이 조회됩니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : code가 null/blank.
- `TERMS_NOT_FOUND (C006, 404)` : 등록되지 않은 약관 코드.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류 (C001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "약관 없음 (C006)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{code}")
    public ResponseEntity<TermsDetailResponseDto> getTermsDetail(
            @Parameter(description = "약관 코드 (예: SERVICE, PRIVACY, MARKETING)")
            @PathVariable String code
    ) {
        return ResponseEntity.ok(termsService.getTermsDetail(code));
    }
}
