package com.example.tmta.auth.verification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "tmta.email.ses", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailVerificationSender implements EmailVerificationSender {

    @Override
    public void sendVerificationCode(String email, String code) {
        // TODO(prod): SMTP/외부 메일 서비스 연동 구현
        log.info("[EmailVerification] target={}, code={}", email, code);
    }
}
