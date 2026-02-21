package com.example.tmta.auth;

import com.example.tmta.auth.dto.EmailVerificationConfirmRequest;
import com.example.tmta.auth.dto.EmailVerificationConfirmResponse;
import com.example.tmta.auth.dto.EmailVerificationSendRequest;
import com.example.tmta.auth.verification.EmailVerificationRecord;
import com.example.tmta.auth.verification.EmailVerificationSender;
import com.example.tmta.auth.verification.EmailVerificationStore;
import com.example.tmta.auth.verification.EmailVerificationTokenProvider;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String FIXED_CODE = "1111";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofMinutes(30);

    private final EmailVerificationStore emailVerificationStore;
    private final EmailVerificationSender emailVerificationSender;
    private final EmailVerificationTokenProvider emailVerificationTokenProvider;

    /** 인증번호를 생성(현재 고정값)하고 저장 후 전송합니다. */
    public void sendVerificationCode(EmailVerificationSendRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        Instant codeExpiresAt = Instant.now().plus(CODE_TTL);
        emailVerificationStore.saveCode(normalizedEmail, FIXED_CODE, codeExpiresAt);
        emailVerificationSender.sendVerificationCode(normalizedEmail, FIXED_CODE);
        // TODO(feature-email-code): 고정 코드(1111)를 난수 코드로 교체하고 해시값으로 저장합니다.
        // TODO(feature-email-rate-limit): 이메일/아이피 단위 발송 횟수 제한과 재전송 쿨다운을 적용합니다.
    }

    /** 인증번호를 검증하고 회원가입 전용 인증 토큰을 발급합니다. */
    public EmailVerificationConfirmResponse confirmVerificationCode(EmailVerificationConfirmRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        EmailVerificationRecord record = emailVerificationStore.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUIRED));
        Instant now = Instant.now();
        if (record.isCodeExpired(now)) {
            emailVerificationStore.clear(normalizedEmail);
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }
        if (!record.code().equals(request.code())) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_MISMATCH);
        }
        String token = emailVerificationTokenProvider.createToken(normalizedEmail, VERIFICATION_TOKEN_TTL);
        emailVerificationStore.clear(normalizedEmail);
        return new EmailVerificationConfirmResponse(true, token);
    }

    /** 회원가입 시 이메일과 인증 토큰의 주체가 일치하는지 검증합니다. */
    public void assertEmailVerifiedForSignUp(String email, String verificationToken) {
        String normalizedEmail = normalizeEmail(email);
        String verifiedEmail = normalizeEmail(emailVerificationTokenProvider.getVerifiedEmail(verificationToken));
        if (!normalizedEmail.equals(verifiedEmail)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }
    }

    /** 이메일 문자열을 트림/소문자 정규화합니다. */
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
