package com.example.tmta.auth;

import com.example.tmta.auth.dto.AuthResponse;
import com.example.tmta.auth.dto.LoginRequest;
import com.example.tmta.auth.dto.SignUpRequest;
import com.example.tmta.auth.dto.SignUpResponse;
import com.example.tmta.entity.Member;
import com.example.tmta.entity.RefreshToken;
import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import com.example.tmta.repository.MemberRepository;
import com.example.tmta.repository.RefreshTokenRepository;
import com.example.tmta.security.JwtProperties;
import com.example.tmta.security.JwtTokenProvider;
import com.example.tmta.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (memberRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);
        }

        Member member = Member.registerLocal(request.email(), passwordEncoder.encode(request.password()));

        // NOTE(social-login): Google/Naver 연동 시 authProvider/providerId 기반으로 계정 매핑 확장 포인트입니다.
        // LOCAL은 password 사용, 소셜 계정은 password 없이 providerId로 식별하면 됩니다.

        // NOTE(email-verification): 이메일 검증 확장 시 검증 토큰을 생성해 메일 발송 큐에 넣고,
        // 토큰 검증 API에서 member.emailVerified=true 처리하세요.
        // 예시 흐름: VerificationToken(entity) 저장 -> 메일 발송 -> /verify?token=... 확인.

        memberRepository.save(member);

        return new SignUpResponse(member.getId(), member.getEmail());
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        try {
            UserPrincipal principal = authenticate(request.email(), request.password());
            Member member = memberRepository.findById(principal.memberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            String accessToken = jwtTokenProvider.createAccessToken(principal);
            String refreshToken = jwtTokenProvider.createRefreshToken(principal);
            LocalDateTime refreshExpiresAt = jwtTokenProvider.getExpiration(refreshToken);

            refreshTokenRepository.findByMember(member)
                    .ifPresentOrElse(
                            existing -> existing.rotate(refreshToken, refreshExpiresAt),
                            () -> refreshTokenRepository.save(RefreshToken.issue(member, refreshToken, refreshExpiresAt))
                    );

            AuthResponse response = new AuthResponse(accessToken, "Bearer", !member.isProfileSetupCompleted());

            return new LoginResult(response, refreshToken, jwtProperties.refreshTokenValiditySeconds());
        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Transactional
    public LoginResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            jwtTokenProvider.validateToken(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = storedToken.getMember();
        UserPrincipal principal = new UserPrincipal(
                member.getId(),
                member.getEmail(),
                member.getPassword(),
                member.getRole().name()
        );

        String newAccessToken = jwtTokenProvider.createAccessToken(principal);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(principal);
        LocalDateTime refreshExpiresAt = jwtTokenProvider.getExpiration(newRefreshToken);

        storedToken.rotate(newRefreshToken, refreshExpiresAt);

        AuthResponse response = new AuthResponse(newAccessToken, "Bearer", !member.isProfileSetupCompleted());

        return new LoginResult(response, newRefreshToken, jwtProperties.refreshTokenValiditySeconds());
    }

    @Transactional
    public void logout(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        refreshTokenRepository.deleteByMember(member);
    }

    public record LoginResult(AuthResponse response, String refreshToken, long refreshTokenValiditySeconds) {
    }

    private UserPrincipal authenticate(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        return (UserPrincipal) authentication.getPrincipal();
    }
}
