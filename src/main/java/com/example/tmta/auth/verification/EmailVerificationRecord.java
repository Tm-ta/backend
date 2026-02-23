package com.example.tmta.auth.verification;

import java.time.Instant;

public record EmailVerificationRecord(
        String codeHash,
        Instant codeExpiresAt,
        int failedAttempts,
        int sendCount,
        Instant firstSentAt,
        Instant lastSentAt
) {
    public boolean isCodeExpired(Instant now) {
        return codeExpiresAt == null || now.isAfter(codeExpiresAt);
    }

    public boolean isResendCooldownActive(Instant now, long cooldownSeconds) {
        if (lastSentAt == null || cooldownSeconds <= 0) {
            return false;
        }
        return now.isBefore(lastSentAt.plusSeconds(cooldownSeconds));
    }

    public boolean isSendRateLimitExceeded(Instant now, long windowSeconds, int maxSendsPerWindow) {
        if (maxSendsPerWindow <= 0) {
            return false;
        }
        if (firstSentAt == null || windowSeconds <= 0) {
            return sendCount >= maxSendsPerWindow;
        }
        if (now.isAfter(firstSentAt.plusSeconds(windowSeconds))) {
            return false;
        }
        return sendCount >= maxSendsPerWindow;
    }

    public boolean isVerifyAttemptsExceeded(int maxVerifyAttempts) {
        return maxVerifyAttempts > 0 && failedAttempts >= maxVerifyAttempts;
    }

    public EmailVerificationRecord nextSend(String nextCodeHash, Instant now, Instant nextCodeExpiresAt, long rateLimitWindowSeconds) {
        boolean resetWindow = firstSentAt == null
                || rateLimitWindowSeconds <= 0
                || now.isAfter(firstSentAt.plusSeconds(rateLimitWindowSeconds));
        Instant nextFirstSentAt = resetWindow ? now : firstSentAt;
        int nextSendCount = resetWindow ? 1 : sendCount + 1;
        return new EmailVerificationRecord(nextCodeHash, nextCodeExpiresAt, 0, nextSendCount, nextFirstSentAt, now);
    }

    public EmailVerificationRecord incrementFailedAttempts() {
        return new EmailVerificationRecord(codeHash, codeExpiresAt, failedAttempts + 1, sendCount, firstSentAt, lastSentAt);
    }
}
