package com.example.tmta.auth.social;

import com.example.tmta.auth.dto.SocialLoginRequest;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.example.tmta.member.entity.type.AuthProvider;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.stereotype.Component;

@Component
public class AppleSocialIdentityVerifier implements SocialIdentityVerifier {

    private final SocialAuthProperties properties;
    private final JwksIdTokenVerifierSupport verifierSupport;

    public AppleSocialIdentityVerifier(SocialAuthProperties properties, JwksIdTokenVerifierSupport verifierSupport) {
        this.properties = properties;
        this.verifierSupport = verifierSupport;
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.APPLE;
    }

    @Override
    public SocialIdentity verify(SocialLoginRequest request) {
        if (!properties.getApple().isEnabled()) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }
        JWTClaimsSet claims = verifierSupport.verify(
                request.providerToken(),
                properties.getApple().getJwksUri(),
                properties.getApple().getIssuer(),
                properties.getApple().getAudiences()
        );
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
        }
        String email = stringClaim(claims, "email");
        if ((email == null || email.isBlank()) && request.emailHint() != null && !request.emailHint().isBlank()) {
            email = request.emailHint();
        }
        return new SocialIdentity(AuthProvider.APPLE, sub, email);
    }

    private String stringClaim(JWTClaimsSet claims, String key) {
        Object value = claims.getClaim(key);
        return value == null ? null : value.toString();
    }
}
