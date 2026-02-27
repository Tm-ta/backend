package com.example.tmta.auth.verification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmta.email.verification")
public class EmailVerificationProperties {

    private int codeLength = 6;
    private long codeTtlSeconds = 300;
    private long verificationTokenTtlSeconds = 1800;
    private long resendCooldownSeconds = 60;
    private long rateLimitWindowSeconds = 3600;
    private int maxSendsPerWindow = 5;
    private int maxVerifyAttempts = 5;

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public long getCodeTtlSeconds() {
        return codeTtlSeconds;
    }

    public void setCodeTtlSeconds(long codeTtlSeconds) {
        this.codeTtlSeconds = codeTtlSeconds;
    }

    public long getVerificationTokenTtlSeconds() {
        return verificationTokenTtlSeconds;
    }

    public void setVerificationTokenTtlSeconds(long verificationTokenTtlSeconds) {
        this.verificationTokenTtlSeconds = verificationTokenTtlSeconds;
    }

    public long getResendCooldownSeconds() {
        return resendCooldownSeconds;
    }

    public void setResendCooldownSeconds(long resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public long getRateLimitWindowSeconds() {
        return rateLimitWindowSeconds;
    }

    public void setRateLimitWindowSeconds(long rateLimitWindowSeconds) {
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
    }

    public int getMaxSendsPerWindow() {
        return maxSendsPerWindow;
    }

    public void setMaxSendsPerWindow(int maxSendsPerWindow) {
        this.maxSendsPerWindow = maxSendsPerWindow;
    }

    public int getMaxVerifyAttempts() {
        return maxVerifyAttempts;
    }

    public void setMaxVerifyAttempts(int maxVerifyAttempts) {
        this.maxVerifyAttempts = maxVerifyAttempts;
    }
}
