package com.example.tmta.auth.social;

import com.example.tmta.member.entity.type.AuthProvider;

public record SocialIdentity(
        AuthProvider provider,
        String providerUserId,
        String email
) {
}
