package com.example.tmta.auth.verification;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"dev", "test"})
public class InMemoryEmailVerificationStore implements EmailVerificationStore {

    private final Map<String, EmailVerificationRecord> storage = new ConcurrentHashMap<>();

    @Override
    public void save(String email, EmailVerificationRecord record) {
        storage.put(email, record);
    }

    @Override
    public Optional<EmailVerificationRecord> findByEmail(String email) {
        return Optional.ofNullable(storage.get(email));
    }

    @Override
    public void clear(String email) {
        storage.remove(email);
    }
}
