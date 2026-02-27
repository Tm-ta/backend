package com.example.tmta.auth.social;

import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwksIdTokenVerifierSupport {

    private final Map<String, ConfigurableJWTProcessor<SecurityContext>> processorCache = new ConcurrentHashMap<>();

    public JWTClaimsSet verify(String rawIdToken,
                               String jwksUri,
                               String issuer,
                               List<String> audiences) {
        if (rawIdToken == null || rawIdToken.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
        }
        if (jwksUri == null || jwksUri.isBlank() || issuer == null || issuer.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }

        try {
            JWTClaimsSet claims = processorCache.computeIfAbsent(jwksUri, this::newProcessor).process(rawIdToken, null);
            validateCommonClaims(claims, issuer, audiences);
            return claims;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
        }
    }

    private ConfigurableJWTProcessor<SecurityContext> newProcessor(String jwksUri) {
        try {
            DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            DefaultResourceRetriever retriever = new DefaultResourceRetriever(2000, 2000, 1024 * 1024);
            JWKSource<SecurityContext> source = new RemoteJWKSet<>(new URL(jwksUri), retriever);
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, source);
            processor.setJWSKeySelector(keySelector);
            return processor;
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }
    }

    private void validateCommonClaims(JWTClaimsSet claims, String issuer, List<String> audiences) {
        String tokenIssuer = claims.getIssuer();
        if (tokenIssuer == null || !issuer.equals(tokenIssuer)) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
        }
        if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
        }
        if (audiences != null && !audiences.isEmpty()) {
            List<String> tokenAud = claims.getAudience();
            boolean matched = tokenAud != null && tokenAud.stream().anyMatch(audiences::contains);
            if (!matched) {
                throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
            }
        }
    }
}
