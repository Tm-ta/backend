package com.example.tmta.auth.verification;

import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import com.example.tmta.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailVerificationTokenProviderTest {

    private static final String SECRET_BASE64 = "VGhpc0lzQVRlbXBEZXZlbG9wbWVudFNlY3JldEtleUZvclRNVEEyMDI2IQ==";

    private final JwtProperties jwtProperties = new JwtProperties(
            SECRET_BASE64,
            3600L,
            1209600L,
            "tmta"
    );

    private final EmailVerificationTokenProvider tokenProvider = new EmailVerificationTokenProvider(jwtProperties);

    @Test
    @DisplayName("정상 토큰은 이메일 subject를 반환한다")
    void getVerifiedEmailSuccess() {
        String token = tokenProvider.createToken("user@test.com", Duration.ofMinutes(10));

        String verifiedEmail = tokenProvider.getVerifiedEmail(token);

        assertThat(verifiedEmail).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("purpose claim이 다르면 EMAIL_VERIFICATION_TOKEN_INVALID 예외")
    void invalidPurpose() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_BASE64));
        Instant now = Instant.now();
        String token = Jwts.builder()
                .issuer("tmta")
                .subject("user@test.com")
                .claim("purpose", "NOT_EMAIL_VERIFICATION")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> tokenProvider.getVerifiedEmail(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
    }

    @Test
    @DisplayName("만료된 토큰이면 EMAIL_VERIFICATION_TOKEN_INVALID 예외")
    void expiredToken() {
        String token = tokenProvider.createToken("user@test.com", Duration.ofSeconds(-1));

        assertThatThrownBy(() -> tokenProvider.getVerifiedEmail(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
    }
}
