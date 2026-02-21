package com.example.tmta.auth.repository;

import com.example.tmta.member.entity.Member;
import com.example.tmta.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByMember(Member member);
    Optional<RefreshToken> findByToken(String token);
    void deleteByMember(Member member);
}
