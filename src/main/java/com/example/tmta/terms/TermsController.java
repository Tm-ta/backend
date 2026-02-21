package com.example.tmta.terms;

import com.example.tmta.terms.dto.TermsDetailResponseDto;
import com.example.tmta.terms.dto.TermsListResponseDto;
import com.example.tmta.terms.TermsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "약관 목록 조회", description = "동의가 필요한 약관 목록(코드/버전/필수 여부)을 조회합니다.")
    @GetMapping
    public ResponseEntity<TermsListResponseDto> getTermsList() {
        return ResponseEntity.ok(termsService.getTermsList());
    }

    @Operation(summary = "약관 상세 조회", description = "약관 코드로 약관 본문을 조회합니다.")
    @GetMapping("/{code}")
    public ResponseEntity<TermsDetailResponseDto> getTermsDetail(
            @Parameter(description = "약관 코드 (예: SERVICE, PRIVACY, MARKETING)")
            @PathVariable String code
    ) {
        return ResponseEntity.ok(termsService.getTermsDetail(code));
    }
}
