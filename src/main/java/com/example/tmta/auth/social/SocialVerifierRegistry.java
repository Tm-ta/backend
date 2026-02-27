package com.example.tmta.auth.social;

import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.example.tmta.member.entity.type.AuthProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SocialVerifierRegistry {

    private final Map<AuthProvider, SocialIdentityVerifier> verifiers = new EnumMap<>(AuthProvider.class);

    public SocialVerifierRegistry(List<SocialIdentityVerifier> verifierList) {
        for (SocialIdentityVerifier verifier : verifierList) {
            verifiers.put(verifier.provider(), verifier);
        }
    }

    public SocialIdentityVerifier get(AuthProvider provider) {
        if (provider == null || provider == AuthProvider.LOCAL) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }
        SocialIdentityVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }
        return verifier;
    }
}
