package com.example.tmta.terms;

import com.example.tmta.terms.dto.TermsDetailResponseDto;
import com.example.tmta.terms.dto.TermsListResponseDto;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TermsService {

    private final Map<String, TermsDocument> termsByCode;

    /** 기본 약관 문서를 메모리에 등록합니다. */
    public TermsService() {
        this.termsByCode = new LinkedHashMap<>();
        register(new TermsDocument(
                "SERVICE",
                "서비스 이용약관",
                true,
                "v1.0",
                LocalDate.of(2026, 2, 20),
                """
                        제1조 (목적)
                        본 약관은 TMTA 서비스의 이용과 관련한 기본 사항을 정합니다.

                        제2조 (서비스 제공)
                        회사는 일정 관리 및 팀 협업 기능을 제공합니다.

                        제3조 (회원의 의무)
                        회원은 관련 법령과 본 약관을 준수해야 합니다.
                        """
        ));
        register(new TermsDocument(
                "PRIVACY",
                "개인정보 처리방침",
                true,
                "v1.0",
                LocalDate.of(2026, 2, 20),
                """
                        1. 수집 항목
                        이메일, 닉네임, 프로필 이미지 등 서비스 제공에 필요한 정보를 수집할 수 있습니다.

                        2. 이용 목적
                        회원 식별, 서비스 제공, 보안 및 운영 안정화 목적에 사용합니다.

                        3. 보관 및 파기
                        관련 법령에 따른 기간 또는 목적 달성 시 지체 없이 파기합니다.
                        """
        ));
        register(new TermsDocument(
                "MARKETING",
                "마케팅 정보 수신 동의",
                false,
                "v1.0",
                LocalDate.of(2026, 2, 20),
                """
                        프로모션, 이벤트 및 신규 기능 안내를 위해 마케팅 정보를 발송할 수 있습니다.
                        본 동의는 선택 사항이며, 언제든지 철회할 수 있습니다.
                        """
        ));
        // TODO(feature-terms-storage): 약관 버전/본문을 DB 또는 외부 CMS에서 조회하도록 전환합니다.
    }

    /** 약관 요약 목록을 반환합니다. */
    public TermsListResponseDto getTermsList() {
        List<TermsListResponseDto.TermsSummary> items = termsByCode.values().stream()
                .map(doc -> new TermsListResponseDto.TermsSummary(
                        doc.code(),
                        doc.title(),
                        doc.required(),
                        doc.version(),
                        doc.effectiveDate()
                ))
                .toList();
        return new TermsListResponseDto(items);
    }

    /** 약관 코드를 기준으로 약관 상세를 반환합니다. */
    public TermsDetailResponseDto getTermsDetail(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        TermsDocument doc = termsByCode.get(code.trim().toUpperCase(Locale.ROOT));
        if (doc == null) {
            throw new BusinessException(ErrorCode.TERMS_NOT_FOUND);
        }
        return new TermsDetailResponseDto(
                doc.code(),
                doc.title(),
                doc.required(),
                doc.version(),
                doc.effectiveDate(),
                doc.content()
        );
    }

    /** 메모리 저장소에 약관 문서를 등록합니다. */
    private void register(TermsDocument document) {
        termsByCode.put(document.code(), document);
    }

    private record TermsDocument(
            String code,
            String title,
            boolean required,
            String version,
            LocalDate effectiveDate,
            String content
    ) {
    }
}
