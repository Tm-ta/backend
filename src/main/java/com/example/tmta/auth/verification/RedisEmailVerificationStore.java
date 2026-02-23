package com.example.tmta.auth.verification;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@Profile("prod")
public class RedisEmailVerificationStore implements EmailVerificationStore {

    private static final String KEY_PREFIX = "email-verification:";
    private static final String CODE_HASH = "codeHash";
    private static final String CODE_EXPIRES_AT = "codeExpiresAt";
    private static final String FAILED_ATTEMPTS = "failedAttempts";
    private static final String SEND_COUNT = "sendCount";
    private static final String FIRST_SENT_AT = "firstSentAt";
    private static final String LAST_SENT_AT = "lastSentAt";

    private final StringRedisTemplate redisTemplate;

    public RedisEmailVerificationStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String email, EmailVerificationRecord record) {
        String key = key(email);
        redisTemplate.opsForHash().putAll(key, Map.of(
                CODE_HASH, nullSafe(record.codeHash()),
                CODE_EXPIRES_AT, toEpochMilli(record.codeExpiresAt()),
                FAILED_ATTEMPTS, Integer.toString(record.failedAttempts()),
                SEND_COUNT, Integer.toString(record.sendCount()),
                FIRST_SENT_AT, toEpochMilli(record.firstSentAt()),
                LAST_SENT_AT, toEpochMilli(record.lastSentAt())
        ));

        long ttlMillis = Math.max(1L, Duration.between(Instant.now(), record.codeExpiresAt()).toMillis());
        redisTemplate.expire(key, Duration.ofMillis(ttlMillis));
    }

    @Override
    public Optional<EmailVerificationRecord> findByEmail(String email) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key(email));
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EmailVerificationRecord(
                asString(values.get(CODE_HASH)),
                parseInstant(values.get(CODE_EXPIRES_AT)),
                parseInt(values.get(FAILED_ATTEMPTS)),
                parseInt(values.get(SEND_COUNT)),
                parseInstant(values.get(FIRST_SENT_AT)),
                parseInstant(values.get(LAST_SENT_AT))
        ));
    }

    @Override
    public void clear(String email) {
        redisTemplate.delete(key(email));
    }

    private String key(String email) {
        return KEY_PREFIX + email;
    }

    private String toEpochMilli(Instant value) {
        return value == null ? "" : Long.toString(value.toEpochMilli());
    }

    private Instant parseInstant(Object value) {
        String raw = asString(value);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Instant.ofEpochMilli(Long.parseLong(raw));
    }

    private int parseInt(Object value) {
        String raw = asString(value);
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        return Integer.parseInt(raw);
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
