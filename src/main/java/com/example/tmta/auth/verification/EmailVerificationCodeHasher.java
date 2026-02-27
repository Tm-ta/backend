package com.example.tmta.auth.verification;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class EmailVerificationCodeHasher {

    private final PasswordEncoder passwordEncoder;

    public EmailVerificationCodeHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String rawCode) {
        return passwordEncoder.encode(rawCode);
    }

    public boolean matches(String rawCode, String hashedCode) {
        return rawCode != null && hashedCode != null && passwordEncoder.matches(rawCode, hashedCode);
    }
}
