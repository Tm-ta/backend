package com.example.tmta.auth.verification;

public interface EmailVerificationSender {
    void sendVerificationCode(String email, String code);
}
