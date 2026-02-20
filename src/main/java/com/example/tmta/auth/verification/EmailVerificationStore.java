package com.example.tmta.auth.verification;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationStore {
    void saveCode(String email, String code, Instant codeExpiresAt);

    Optional<EmailVerificationRecord> findByEmail(String email);

    void clear(String email);
}
