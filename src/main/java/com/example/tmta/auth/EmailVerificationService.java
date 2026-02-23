package com.example.tmta.auth;

import com.example.tmta.auth.dto.EmailVerificationConfirmRequest;
import com.example.tmta.auth.dto.EmailVerificationConfirmResponse;
import com.example.tmta.auth.dto.EmailVerificationSendRequest;
import com.example.tmta.auth.verification.EmailVerificationRecord;
import com.example.tmta.auth.verification.EmailVerificationCodeGenerator;
import com.example.tmta.auth.verification.EmailVerificationCodeHasher;
import com.example.tmta.auth.verification.EmailVerificationProperties;
import com.example.tmta.auth.verification.EmailVerificationSender;
import com.example.tmta.auth.verification.EmailVerificationStore;
import com.example.tmta.auth.verification.EmailVerificationTokenProvider;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationStore emailVerificationStore;
    private final EmailVerificationSender emailVerificationSender;
    private final EmailVerificationTokenProvider emailVerificationTokenProvider;
    private final EmailVerificationProperties emailVerificationProperties;
    private final EmailVerificationCodeGenerator codeGenerator;
    private final EmailVerificationCodeHasher codeHasher;

    /** 인증번호를 생성하고 저장 후 전송합니다. */
    public void sendVerificationCode(EmailVerificationSendRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        Instant now = Instant.now();
        var existingRecord = emailVerificationStore.findByEmail(normalizedEmail);

        existingRecord.ifPresent(existing -> {
            if (!existing.isCodeExpired(now)
                    && existing.isResendCooldownActive(now, emailVerificationProperties.getResendCooldownSeconds())) {
                throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_RESEND_COOLDOWN);
            }
            if (existing.isSendRateLimitExceeded(
                    now,
                    emailVerificationProperties.getRateLimitWindowSeconds(),
                    emailVerificationProperties.getMaxSendsPerWindow()
            )) {
                throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_SEND_LIMIT_EXCEEDED);
            }
        });

        String code = codeGenerator.generateNumericCode(emailVerificationProperties.getCodeLength());
        Instant codeExpiresAt = now.plusSeconds(emailVerificationProperties.getCodeTtlSeconds());
        String codeHash = codeHasher.hash(code);

        EmailVerificationRecord nextRecord = existingRecord
                .map(existing -> existing.nextSend(
                        codeHash,
                        now,
                        codeExpiresAt,
                        emailVerificationProperties.getRateLimitWindowSeconds()
                ))
                .orElseGet(() -> new EmailVerificationRecord(
                        codeHash,
                        codeExpiresAt,
                        0,
                        1,
                        now,
                        now
                ));

        emailVerificationStore.save(normalizedEmail, nextRecord);
        emailVerificationSender.sendVerificationCode(normalizedEmail, code);
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
        if (record.isVerifyAttemptsExceeded(emailVerificationProperties.getMaxVerifyAttempts())) {
            emailVerificationStore.clear(normalizedEmail);
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED);
        }
        if (!codeHasher.matches(request.code(), record.codeHash())) {
            EmailVerificationRecord updated = record.incrementFailedAttempts();
            if (updated.isVerifyAttemptsExceeded(emailVerificationProperties.getMaxVerifyAttempts())) {
                emailVerificationStore.clear(normalizedEmail);
                throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED);
            }
            emailVerificationStore.save(normalizedEmail, updated);
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_MISMATCH);
        }
        String token = emailVerificationTokenProvider.createToken(
                normalizedEmail,
                java.time.Duration.ofSeconds(emailVerificationProperties.getVerificationTokenTtlSeconds())
        );
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
