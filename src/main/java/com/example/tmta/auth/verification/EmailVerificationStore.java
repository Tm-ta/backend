package com.example.tmta.auth.verification;

import java.util.Optional;

public interface EmailVerificationStore {
    void save(String email, EmailVerificationRecord record);

    Optional<EmailVerificationRecord> findByEmail(String email);

    void clear(String email);
}
