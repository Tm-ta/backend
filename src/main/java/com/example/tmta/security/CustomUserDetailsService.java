package com.example.tmta.security;

import com.example.tmta.entity.Member;
import com.example.tmta.entity.type.AuthProvider;
import com.example.tmta.repository.MemberRepository;
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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found"));

        if (member.getAuthProvider() != AuthProvider.LOCAL) {
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
