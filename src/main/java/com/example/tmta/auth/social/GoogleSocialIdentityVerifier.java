package com.example.tmta.auth.social;

import com.example.tmta.auth.dto.SocialLoginRequest;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.example.tmta.member.entity.type.AuthProvider;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.stereotype.Component;

@Component
public class GoogleSocialIdentityVerifier implements SocialIdentityVerifier {

    private final SocialAuthProperties properties;
    private final JwksIdTokenVerifierSupport verifierSupport;

    public GoogleSocialIdentityVerifier(SocialAuthProperties properties, JwksIdTokenVerifierSupport verifierSupport) {
        this.properties = properties;
        this.verifierSupport = verifierSupport;
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public SocialIdentity verify(SocialLoginRequest request) {
        if (!properties.getGoogle().isEnabled()) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }
        JWTClaimsSet claims = verifierSupport.verify(
                request.providerToken(),
                properties.getGoogle().getJwksUri(),
                properties.getGoogle().getIssuer(),
                properties.getGoogle().getAudiences()
        );
        String sub = claims.getSubject();
        String email = stringClaim(claims, "email");
        if (sub == null || sub.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
        }
        Object emailVerified = claims.getClaim("email_verified");
        if (emailVerified != null && !Boolean.parseBoolean(String.valueOf(emailVerified))) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
        }
        return new SocialIdentity(AuthProvider.GOOGLE, sub, email);
    }

    private String stringClaim(JWTClaimsSet claims, String key) {
        Object value = claims.getClaim(key);
        return value == null ? null : value.toString();
    }
}
