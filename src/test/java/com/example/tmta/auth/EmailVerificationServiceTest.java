package com.example.tmta.auth;

import com.example.tmta.auth.dto.EmailVerificationConfirmRequest;
import com.example.tmta.auth.dto.EmailVerificationSendRequest;
import com.example.tmta.auth.verification.EmailVerificationRecord;
import com.example.tmta.auth.verification.EmailVerificationSender;
import com.example.tmta.auth.verification.EmailVerificationStore;
import com.example.tmta.auth.verification.EmailVerificationTokenProvider;
import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
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

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Nested
    @DisplayName("sendVerificationCode")
    class SendVerificationCode {

        @Test
        @DisplayName("이메일 정규화 후 코드 저장 및 발송")
        void sendCodeSuccess() {
            Instant before = Instant.now();

            emailVerificationService.sendVerificationCode(new EmailVerificationSendRequest(" User@Test.com "));

            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Instant> expiresAtCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(emailVerificationStore).saveCode(emailCaptor.capture(), codeCaptor.capture(), expiresAtCaptor.capture());

            assertThat(emailCaptor.getValue()).isEqualTo("user@test.com");
            assertThat(codeCaptor.getValue()).isEqualTo("1111");
            assertThat(expiresAtCaptor.getValue()).isAfter(before.plus(Duration.ofMinutes(4)));
            assertThat(expiresAtCaptor.getValue()).isBefore(before.plus(Duration.ofMinutes(6)));
            verify(emailVerificationSender).sendVerificationCode("user@test.com", "1111");
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
            when(emailVerificationStore.findByEmail("user@test.com"))
                    .thenReturn(Optional.of(new EmailVerificationRecord("1111", Instant.now().minusSeconds(1))));

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
            when(emailVerificationStore.findByEmail("user@test.com"))
                    .thenReturn(Optional.of(new EmailVerificationRecord("1111", Instant.now().plusSeconds(300))));

            assertThatThrownBy(() -> emailVerificationService.confirmVerificationCode(
                    new EmailVerificationConfirmRequest("user@test.com", "9999")
            ))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_VERIFICATION_MISMATCH);

            verify(emailVerificationStore, never()).clear("user@test.com");
        }

        @Test
        @DisplayName("코드 검증 성공 시 회원가입용 토큰 발급")
        void confirmSuccess() {
            when(emailVerificationStore.findByEmail("user@test.com"))
                    .thenReturn(Optional.of(new EmailVerificationRecord("1111", Instant.now().plusSeconds(300))));
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
