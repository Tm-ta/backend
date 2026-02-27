package com.example.tmta.auth;

import com.example.tmta.auth.dto.EmailVerificationConfirmRequest;
import com.example.tmta.auth.dto.EmailVerificationSendRequest;
import com.example.tmta.auth.verification.EmailVerificationCodeGenerator;
import com.example.tmta.auth.verification.EmailVerificationCodeHasher;
import com.example.tmta.auth.verification.EmailVerificationProperties;
import com.example.tmta.auth.verification.EmailVerificationRecord;
import com.example.tmta.auth.verification.EmailVerificationSender;
import com.example.tmta.auth.verification.EmailVerificationStore;
import com.example.tmta.auth.verification.EmailVerificationTokenProvider;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationStore emailVerificationStore;
    @Mock
    private EmailVerificationSender emailVerificationSender;
    @Mock
    private EmailVerificationTokenProvider emailVerificationTokenProvider;
    @Mock
    private EmailVerificationProperties emailVerificationProperties;
    @Mock
    private EmailVerificationCodeGenerator codeGenerator;
    @Mock
    private EmailVerificationCodeHasher codeHasher;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private void stubDefaultVerificationProperties() {
        lenient().when(emailVerificationProperties.getCodeLength()).thenReturn(6);
        lenient().when(emailVerificationProperties.getCodeTtlSeconds()).thenReturn(300L);
        lenient().when(emailVerificationProperties.getVerificationTokenTtlSeconds()).thenReturn(1800L);
        lenient().when(emailVerificationProperties.getResendCooldownSeconds()).thenReturn(60L);
        lenient().when(emailVerificationProperties.getRateLimitWindowSeconds()).thenReturn(3600L);
        lenient().when(emailVerificationProperties.getMaxSendsPerWindow()).thenReturn(5);
        lenient().when(emailVerificationProperties.getMaxVerifyAttempts()).thenReturn(5);
    }

    private EmailVerificationRecord activeRecord(String hash) {
        Instant now = Instant.now();
        return new EmailVerificationRecord(hash, now.plusSeconds(300), 0, 1, now.minusSeconds(60), now.minusSeconds(30));
    }

    @Nested
    @DisplayName("sendVerificationCode")
    class SendVerificationCode {

        @Test
        @DisplayName("이메일 정규화 후 코드 저장 및 발송")
        void sendCodeSuccess() {
            Instant before = Instant.now();
            stubDefaultVerificationProperties();
            when(emailVerificationStore.findByEmail("user@test.com")).thenReturn(Optional.empty());
            when(codeGenerator.generateNumericCode(6)).thenReturn("123456");
            when(codeHasher.hash("123456")).thenReturn("hashed-code");

            emailVerificationService.sendVerificationCode(new EmailVerificationSendRequest(" User@Test.com "));

            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<EmailVerificationRecord> recordCaptor = ArgumentCaptor.forClass(EmailVerificationRecord.class);
            verify(emailVerificationStore).save(emailCaptor.capture(), recordCaptor.capture());

            assertThat(emailCaptor.getValue()).isEqualTo("user@test.com");
            assertThat(recordCaptor.getValue().codeHash()).isEqualTo("hashed-code");
            assertThat(recordCaptor.getValue().codeExpiresAt()).isAfter(before.plus(Duration.ofMinutes(4)));
            assertThat(recordCaptor.getValue().codeExpiresAt()).isBefore(before.plus(Duration.ofMinutes(6)));
            assertThat(recordCaptor.getValue().failedAttempts()).isZero();
            assertThat(recordCaptor.getValue().sendCount()).isEqualTo(1);
            verify(emailVerificationSender).sendVerificationCode("user@test.com", "123456");
        }

        @Test
        @DisplayName("쿨다운 내 재요청이면 EMAIL_VERIFICATION_RESEND_COOLDOWN 예외")
        void resendCooldown() {
            stubDefaultVerificationProperties();
            Instant now = Instant.now();
            when(emailVerificationStore.findByEmail("user@test.com"))
                    .thenReturn(Optional.of(new EmailVerificationRecord(
                            "hashed", now.plusSeconds(300), 0, 1, now.minusSeconds(10), now.minusSeconds(10)
                    )));

            assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(new EmailVerificationSendRequest("user@test.com")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_VERIFICATION_RESEND_COOLDOWN);

            verify(emailVerificationSender, never()).sendVerificationCode(any(), any());
        }

        @Test
        @DisplayName("이메일이 공백이면 INVALID_INPUT_VALUE 예외")
        void sendCodeBlankEmail() {
            assertThatThrownBy(() -> emailVerificationService.sendVerificationCode(new EmailVerificationSendRequest("  ")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Nested
    @DisplayName("confirmVerificationCode")
    class ConfirmVerificationCode {

        @Test
        @DisplayName("저장된 인증정보가 없으면 EMAIL_VERIFICATION_REQUIRED 예외")
        void missingRecord() {
            stubDefaultVerificationProperties();
            when(emailVerificationStore.findByEmail("user@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailVerificationService.confirmVerificationCode(
                    new EmailVerificationConfirmRequest("user@test.com", "1111")
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }

        @Test
        @DisplayName("인증코드가 만료되면 저장소에서 제거하고 EMAIL_VERIFICATION_EXPIRED 예외")
        void expiredCode() {
            stubDefaultVerificationProperties();
            Instant now = Instant.now();
            when(emailVerificationStore.findByEmail("user@test.com"))
                    .thenReturn(Optional.of(new EmailVerificationRecord(
                            "hashed", now.minusSeconds(1), 0, 1, now.minusSeconds(60), now.minusSeconds(30)
                    )));

            assertThatThrownBy(() -> emailVerificationService.confirmVerificationCode(
                    new EmailVerificationConfirmRequest("user@test.com", "1111")
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_VERIFICATION_EXPIRED);

            verify(emailVerificationStore).clear("user@test.com");
        }

        @Test
        @DisplayName("인증코드가 다르면 EMAIL_VERIFICATION_MISMATCH 예외")
        void mismatchCode() {
            stubDefaultVerificationProperties();
            when(emailVerificationStore.findByEmail("user@test.com"))
                    .thenReturn(Optional.of(activeRecord("hashed")));
            when(codeHasher.matches("9999", "hashed")).thenReturn(false);

            assertThatThrownBy(() -> emailVerificationService.confirmVerificationCode(
                    new EmailVerificationConfirmRequest("user@test.com", "9999")
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_VERIFICATION_MISMATCH);

            verify(emailVerificationStore, never()).clear("user@test.com");
            verify(emailVerificationStore).save(eq("user@test.com"), any(EmailVerificationRecord.class));
        }

        @Test
        @DisplayName("실패 횟수 초과 시 EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED 예외")
        void attemptLimitExceeded() {
            stubDefaultVerificationProperties();
            Instant now = Instant.now();
            when(emailVerificationStore.findByEmail("user@test.com"))
                    .thenReturn(Optional.of(new EmailVerificationRecord(
                            "hashed", now.plusSeconds(300), 5, 1, now.minusSeconds(60), now.minusSeconds(30)
                    )));

            assertThatThrownBy(() -> emailVerificationService.confirmVerificationCode(
                    new EmailVerificationConfirmRequest("user@test.com", "1111")
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_VERIFICATION_ATTEMPT_LIMIT_EXCEEDED);

            verify(emailVerificationStore).clear("user@test.com");
        }

        @Test
        @DisplayName("코드 검증 성공 시 회원가입용 토큰 발급")
        void confirmSuccess() {
            stubDefaultVerificationProperties();
            when(emailVerificationStore.findByEmail("user@test.com")).thenReturn(Optional.of(activeRecord("hashed")));
            when(codeHasher.matches("1111", "hashed")).thenReturn(true);
            when(emailVerificationTokenProvider.createToken("user@test.com", Duration.ofMinutes(30)))
                    .thenReturn("verification-token");

            var response = emailVerificationService.confirmVerificationCode(
                    new EmailVerificationConfirmRequest("user@test.com", "1111")
            );

            assertThat(response.verified()).isTrue();
            assertThat(response.verificationToken()).isEqualTo("verification-token");
            verify(emailVerificationStore).clear("user@test.com");
        }
    }

    @Nested
    @DisplayName("assertEmailVerifiedForSignUp")
    class AssertEmailVerifiedForSignUp {

        @Test
        @DisplayName("토큰 subject 이메일과 회원가입 이메일이 일치하면 통과")
        void assertVerifiedSuccess() {
            when(emailVerificationTokenProvider.getVerifiedEmail("token")).thenReturn("USER@Test.com");

            emailVerificationService.assertEmailVerifiedForSignUp(" user@test.com ", "token");

            verify(emailVerificationTokenProvider).getVerifiedEmail("token");
        }

        @Test
        @DisplayName("토큰 이메일과 가입 이메일이 다르면 EMAIL_VERIFICATION_REQUIRED 예외")
        void assertVerifiedMismatch() {
            when(emailVerificationTokenProvider.getVerifiedEmail("token")).thenReturn("other@test.com");

            assertThatThrownBy(() -> emailVerificationService.assertEmailVerifiedForSignUp("user@test.com", "token"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
        }
    }
}
