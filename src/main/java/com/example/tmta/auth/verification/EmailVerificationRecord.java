package com.example.tmta.auth.verification;

import java.time.Instant;

public record EmailVerificationRecord(
        String code,
        Instant codeExpiresAt
) {
    public boolean isCodeExpired(Instant now) {
        return codeExpiresAt == null || now.isAfter(codeExpiresAt);
    }
}
