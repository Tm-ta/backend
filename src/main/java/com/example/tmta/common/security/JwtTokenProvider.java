package com.example.tmta.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    public String createAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenValiditySeconds());

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(principal.email())
                .claim("memberId", principal.memberId())
                .claim("role", principal.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.refreshTokenValiditySeconds());

        return Jwts.builder()
                .issuer(jwtProperties.issuer())
                .subject(principal.email())
                .claim("memberId", principal.memberId())
                .claim("role", principal.role())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
        return true;
    }

    public UserPrincipal getPrincipal(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

        Long memberId = claims.get("memberId", Number.class).longValue();
        String role = claims.get("role", String.class);
        String email = claims.getSubject();

        return new UserPrincipal(memberId, email, "", role == null ? "GENERAL" : role);
    }

    public LocalDateTime getExpiration(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        return LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneOffset.UTC);
    }
}
