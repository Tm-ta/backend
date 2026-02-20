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
    private final EmailVerificationService emailVerificationService;

    /** 이메일 인증 토큰이 검증된 사용자만 로컬 회원가입을 처리합니다. */
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        emailVerificationService.assertEmailVerifiedForSignUp(request.email(), request.verificationToken());

        if (memberRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);
        }

        Member member = Member.registerLocal(request.email(), passwordEncoder.encode(request.password()));

        // TODO(feature-social-login): Google/Naver OAuth 로그인 연동 및 providerId 계정 매핑을 구현합니다.
        // TODO(feature-email-verification): 이메일 인증 이력/정책(재시도 제한, 감사로그) 저장소를 외부 DB로 확장합니다.

        memberRepository.save(member);

        return new SignUpResponse(member.getId(), member.getEmail());
    }

    /** 로컬 계정 인증 후 Access/Refresh 토큰을 발급합니다. */
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

    /** Refresh 토큰을 검증하고 Access/Refresh 토큰을 재발급합니다. */
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

    /** 현재 로그인 사용자의 Refresh 토큰을 폐기합니다. */
    @Transactional
    public void logout(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        refreshTokenRepository.deleteByMember(member);
    }

    public record LoginResult(AuthResponse response, String refreshToken, long refreshTokenValiditySeconds) {
    }

    /** 이메일/비밀번호 기반 인증을 수행하고 인증 주체를 반환합니다. */
    private UserPrincipal authenticate(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        return (UserPrincipal) authentication.getPrincipal();
    }
}
