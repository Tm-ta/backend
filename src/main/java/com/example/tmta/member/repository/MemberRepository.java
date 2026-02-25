package com.example.tmta.member.repository;

import com.example.tmta.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByAuthProviderAndProviderId(com.example.tmta.member.entity.type.AuthProvider authProvider, String providerId);
}
