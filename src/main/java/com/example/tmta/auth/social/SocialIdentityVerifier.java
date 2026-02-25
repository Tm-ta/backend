package com.example.tmta.auth.social;

import com.example.tmta.auth.dto.SocialLoginRequest;
import com.example.tmta.member.entity.type.AuthProvider;

public interface SocialIdentityVerifier {
    AuthProvider provider();

    SocialIdentity verify(SocialLoginRequest request);
}
