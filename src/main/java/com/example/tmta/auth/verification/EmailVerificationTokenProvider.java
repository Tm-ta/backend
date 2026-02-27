package com.example.tmta.auth.verification;

import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.example.tmta.common.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class EmailVerificationTokenProvider {

    private static final String PURPOSE_KEY = "purpose";
    private static final String PURPOSE_VALUE = "EMAIL_SIGNUP_VERIFICATION";
    private static final String PASSWORD_RESET_PURPOSE_VALUE = "PASSWORD_RESET_VERIFICATION";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public EmailVerificationTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String createToken(String email, Duration ttl) {
        return createToken(email, ttl, PURPOSE_VALUE);
    }

    public String createPasswordResetToken(String email, Duration ttl) {
        return createToken(email, ttl, PASSWORD_RESET_PURPOSE_VALUE);
    }

    private String createToken(String email, Duration ttl, String purpose) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(email)
                .claim(PURPOSE_KEY, purpose)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public String getVerifiedEmail(String token) {
        return getVerifiedEmail(token, PURPOSE_VALUE, ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
    }

    public String getPasswordResetEmail(String token) {
        return getVerifiedEmail(token, PASSWORD_RESET_PURPOSE_VALUE, ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
    }

    private String getVerifiedEmail(String token, String expectedPurpose, ErrorCode invalidTokenCode) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            Object purpose = claims.get(PURPOSE_KEY);
            if (!expectedPurpose.equals(purpose)) {
                throw new BusinessException(invalidTokenCode);
            }
            String email = claims.getSubject();
            if (email == null || email.isBlank()) {
                throw new BusinessException(invalidTokenCode);
            }
            return email;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(invalidTokenCode);
        }
    }
}
