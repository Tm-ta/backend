package com.example.tmta.common.security;

import com.example.tmta.member.entity.Member;
import com.example.tmta.member.entity.type.AuthProvider;
import com.example.tmta.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    /** 이메일 기반 사용자 조회 후 Spring Security 인증 주체를 생성합니다. */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found"));

        if (member.getAuthProvider() != AuthProvider.LOCAL) {
            // TODO(feature-social-login): 소셜 로그인 계정은 OAuth 인증 흐름으로 분기하도록 구현합니다.
            throw new BadCredentialsException("Invalid auth provider");
        }

        return new UserPrincipal(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                member.getRole().name()
        );
    }
}
