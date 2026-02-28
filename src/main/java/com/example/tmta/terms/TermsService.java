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

    private static final String SERVICE_TERMS_MARKDOWN = """
            # 서비스 이용약관

            **시행일:** 2026.02.20
            **운영자(회사):** 틈타(Tmta) (이하 “회사”)
            **서비스명:** 틈타 (이하 “서비스”)

            ### 제1조 (목적)
            이 약관은 회사가 제공하는 서비스의 이용과 관련하여 회사와 이용자 간 권리·의무 및 책임사항을 정함을 목적으로 합니다.

            ### 제2조 (정의)
            1. “이용자(회원)”란 본 약관에 동의하고 계정을 생성하여 로그인 후 서비스를 이용하는 자를 말합니다.
            2. “약속/그룹/일정/투표” 등은 서비스 내에서 이용자가 생성하거나 참여하는 데이터 단위를 말합니다.
            3. “초대 링크”란 약속/그룹 참여를 위해 생성되는 공유용 링크(또는 코드)를 말합니다.

            ### 제3조 (약관의 효력 및 변경)
            1. 본 약관은 이용자가 동의함으로써 효력이 발생합니다.
            2. 회사는 관련 법령을 위반하지 않는 범위에서 약관을 변경할 수 있으며, 변경 시 시행일 및 주요 내용을 서비스 내 공지(또는 이메일 등)로 안내합니다.
            3. 이용자가 변경 약관 시행 이후에도 서비스를 계속 이용하면 변경에 동의한 것으로 봅니다.

            ### 제4조 (서비스 제공 및 고도화)
            1. 회사는 약속 생성, 참여자 초대, 시간 투표/조율, 결과 공유 등 일정 조율 기능을 제공합니다.
            2. 회사는 서비스 고도화를 위해 AI 기술 및 데이터 분석(A/B 테스트 등)을 활용할 수 있으며, 운영상 필요할 경우 제공 기능의 전부 또는 일부를 변경할 수 있습니다.

            ### 제5조 (회원가입 및 계정)
            1. 회원가입은 이용자가 회사가 정한 절차에 따라 신청하고, 회사가 이를 승인함으로써 완료됩니다. 본 서비스는 만 14세 이상에 한하여 가입이 가능합니다.
            2. 이용자는 정확한 정보를 제공해야 하며, 정보가 부정확하여 발생한 불이익은 이용자 책임입니다.
            3. 이용자는 계정 정보(로그인 수단 포함)를 본인이 관리해야 하며, 제3자에게 양도/대여할 수 없습니다.

            ### 제6조 (이용자의 의무)
            이용자는 아래 행위를 해서는 안 됩니다.
            1. 타인의 계정/정보 도용, 사칭
            2. 서비스 장애 유발(비정상 트래픽, 해킹 시도 등)
            3. 불법·유해 정보 게시/전송, 타인 비방/괴롭힘
            4. 초대 링크/코드를 무단 수집·유포하여 스팸/홍보에 이용
            5. 회사 및 제3자의 권리를 침해하는 행위

            ### 제7조 (데이터 및 콘텐츠)
            1. 이용자가 서비스에 입력한 일정/닉네임/메시지 등 데이터의 권리는 이용자에게 있습니다.
            2. 회사는 서비스 제공, 안정적 운영, AI 모델 학습 및 서비스 개선(A/B 테스트 등)을 위해 필요한 범위에서 해당 데이터를 처리할 수 있습니다. 단, 개인정보는 비식별화 처리함을 원칙으로 합니다. (세부는 개인정보 처리방침에 따름)

            ### 제8조 (서비스 이용 제한 및 해지)
            1. 회사는 이용자가 약관을 위반하거나 서비스 운영에 지장을 주는 경우, 사전 통지 후 이용 제한(기능 제한/정지/해지)을 할 수 있습니다.
            2. 이용자는 언제든 계정 삭제 등으로 이용계약을 종료할 수 있습니다.
            3. 관련 법령/정책상 보관이 필요한 정보는 해당 기간 동안 별도 보관 후 파기합니다.

            ### 제9조 (면책 및 책임 제한)
            1. 회사는 천재지변, 통신 장애, 제3자 서비스 장애 등 불가항력 사유로 서비스를 제공할 수 없는 경우 책임을 지지 않습니다.
            2. 회사는 이용자 간 일정 합의 불일치, 초대 링크 공유로 인한 분쟁 등 이용자 간 문제에 개입하지 않으며, 법령상 의무가 없는 한 책임을 지지 않습니다.
            3. 무료로 제공되는 서비스에 대해, 회사의 고의 또는 중대한 과실이 없는 한 손해배상 책임을 지지 않습니다.

            ### 제10조 (분쟁 해결 및 관할)
            1. 회사와 이용자 간 분쟁은 상호 협의하여 해결하도록 노력합니다.
            2. 협의가 어려운 경우, 대한민국 법령을 적용하며 관할 법원은 민사소송법에 따릅니다.

            ### 제11조 (문의)
            * 고객 문의: hey.Tmta@gmail.com
            * 운영시간: 24시간
            """;

    private static final String PRIVACY_POLICY_MARKDOWN = """
            # 개인정보 처리방침

            **시행일:** 2026.02.20
            **운영자:** 틈타(Tmta)

            ### 1. 수집하는 개인정보 항목
            **(1) 회원가입 및 로그인**
            * (필수) 이름(본명), 닉네임, 이메일 주소
            * (필수) 휴대전화번호, 휴대전화 인증 정보 (인증 시)
            * (자동수집) 기기정보(OS, 앱 버전 등), 로그정보(접속/이용기록, IP 등)

            **(2) 캘린더 연동 (해당 기능 이용 시)**
            * (선택) 캘린더 접근 권한, 캘린더 일정 정보(연동 범위 내)

            ### 2. 개인정보 수집 및 이용 목적
            * 회원가입, 본인확인, 중복가입 및 부정이용 방지
            * 로그인(카카오/구글/애플/이메일) 제공 및 계정 관리
            * 약속/일정 조율 서비스 제공, 참여자 초대 안내
            * 서비스 품질 개선, 오류 분석, AI 학습 및 알고리즘 고도화(A/B 테스트 등)
            * 고객문의 대응 및 공지 전달
            * (선택 동의 시) 이벤트/혜택 등 마케팅 정보 제공
            * (선택 동의 시) 캘린더 연동 기능 제공(일정 조회 등)

            ### 3. 보유 및 이용기간
            * **원칙:** 이용 목적 달성(회원 탈퇴) 시 지체 없이 파기합니다.
            * **로그 정보:** 보안 및 분쟁 대응 목적으로 **6개월 보관 후 파기**합니다.
            * 법령에 따라 보관이 필요한 경우 해당 법령에서 정한 기간 동안 보관합니다.

            ### 4. 제3자 제공
            원칙적으로 이용자의 개인정보를 외부(제3자)에 제공하지 않으며, 법령상 근거가 있거나 사전 동의를 받은 경우에만 제공합니다.

            ### 5. 처리위탁
            회사는 원활한 서비스 제공을 위해 아래와 같이 업무를 위탁하고 있습니다.
            * 소셜 로그인: 카카오, 구글, 애플, 네이버
            * 인프라/호스팅: AWS
            * 푸시/알림 발송: Google LLC (Firebase), Apple Inc. (APNs)

            ### 6. 이용자의 권리
            이용자는 언제든지 본인의 개인정보 열람/정정/삭제/처리정지/동의철회를 요구할 수 있으며, 이는 앱 내 설정 또는 고객문의를 통해 가능합니다.

            ### 7. 파기 절차 및 방법
            * **전자적 파일:** 복구할 수 없는 기술적 방식을 사용하여 영구 삭제합니다.
            * **종이 문서:** 분쇄기로 분쇄하거나 소각합니다.

            ### 8. 안전성 확보 조치
            회사는 개인정보 보호를 위해 접근권한 관리, 접근기록 보관, 데이터 암호화, 보안 시스템 업데이트 등의 조치를 취하고 있습니다.

            ### 9. 캘린더 연동 데이터 처리
            캘린더 연동 기능 제공을 위해 접근 권한을 사용하며, 일정 정보는 기능 제공 범위(일정 조율 및 중복 방지) 내에서만 처리하고 **서버에 영구 저장하지 않습니다.** 연동 해제 또는 회원 탈퇴 시 해당 권한 및 접근 데이터는 즉시 파기됩니다.

            ### 10. 문의처
            * 개인정보 보호 책임 및 고객 문의: hey.Tmta@gmail.com
            """;

    private static final String AGE14_TERMS_MARKDOWN = """
            # 만 14세 이상입니다 (또는 법정대리인 동의가 필요합니다)

            **시행일:** 2026.02.20
            **운영자:** 틈타(Tmta)

            본인은 만 14세 이상임을 확인합니다.
            만 14세 미만인 경우, 법정대리인의 동의가 필요하며 관련 법령에 따라 서비스 이용이 제한될 수 있습니다.
            """;

    private static final String MARKETING_TERMS_MARKDOWN = """
            # 마케팅 정보 수신 동의 (선택)

            **시행일:** 2026.02.20
            **운영자:** 틈타(Tmta) ("회사")

            ### 제1조 (목적)
            본 동의서는 회사가 이용자에게 서비스 관련 이벤트, 프로모션 및 맞춤형 안내 등 마케팅 정보를 제공하기 위해 「정보통신망 이용촉진 및 정보보호 등에 관한 법률」에 따라 사전 동의를 받기 위한 것입니다.

            ### 제2조 (수신 항목 및 채널)
            회사가 마케팅 정보 제공을 위해 처리하는 항목 및 채널은 다음과 같습니다.
            1. **수신 항목:** 이메일 주소, 푸시 토큰(알림 발송용 기기 식별값)
            2. **수신 채널:** 이메일, 앱(기기) 푸시 알림

            ### 제3조 (마케팅 정보의 내용)
            발송되는 마케팅 정보는 아래와 같습니다.
            1. 이벤트, 프로모션, 할인 및 쿠폰 등 혜택 안내
            2. 신규 기능, 업데이트, 서비스 개선 관련 안내 (마케팅 성격 포함)
            3. 이용자 관심사 기반 추천 및 맞춤 혜택 안내

            ### 제4조 (보유 및 이용 기간)
            회사는 이용자의 마케팅 수신 동의일로부터 **동의 철회 시(또는 회원 탈퇴 시)까지** 해당 정보를 보유 및 이용합니다. 단, 관련 법령에 따라 보관이 필요한 경우 정해진 기간 동안 보관합니다.

            ### 제5조 (동의 거부 권리 및 불이익)
            본 동의는 선택 사항이며, 동의하지 않아도 회원가입 및 기본 서비스 이용에는 전혀 제한이 없습니다. 단, 미동의 시 이벤트 및 맞춤형 혜택 안내를 받으실 수 없습니다.

            ### 제6조 (동의 철회 및 수신 거부)
            이용자는 언제든지 아래의 방법으로 수신 동의를 철회할 수 있습니다.
            * **고객센터 문의:** hey.Tmta@gmail.com 으로 수신 거부 의사 전달
            * **이메일:** 수신한 이메일 하단의 '수신 거부(Unsubscribe)' 기능 이용
            * **기기 설정:** 스마트폰 기기 설정에서 틈타(Tmta) 앱의 알림(푸시) 권한 직접 해제

            ### 제7조 (문의처)
            * 문의 이메일: hey.Tmta@gmail.com
            """;

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
                SERVICE_TERMS_MARKDOWN
        ));
        register(new TermsDocument(
                "PRIVACY",
                "개인정보 처리방침",
                true,
                "v1.0",
                LocalDate.of(2026, 2, 20),
                PRIVACY_POLICY_MARKDOWN
        ));
        register(new TermsDocument(
                "AGE14",
                "만 14세 이상입니다 (또는 법정대리인 동의가 필요합니다)",
                true,
                "v1.0",
                LocalDate.of(2026, 2, 20),
                AGE14_TERMS_MARKDOWN
        ));
        register(new TermsDocument(
                "MARKETING",
                "마케팅 정보 수신 동의 (이메일/푸시, 선택)",
                false,
                "v1.0",
                LocalDate.of(2026, 2, 20),
                MARKETING_TERMS_MARKDOWN
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
