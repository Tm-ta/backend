package com.example.tmta.auth.verification;

import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import com.example.tmta.security.JwtProperties;
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

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public EmailVerificationTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String createToken(String email, Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(email)
                .claim(PURPOSE_KEY, PURPOSE_VALUE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public String getVerifiedEmail(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            Object purpose = claims.get(PURPOSE_KEY);
            if (!PURPOSE_VALUE.equals(purpose)) {
                throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
            }
            String email = claims.getSubject();
            if (email == null || email.isBlank()) {
                throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
            }
            return email;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_TOKEN_INVALID);
        }
    }
}
