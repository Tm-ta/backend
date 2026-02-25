package com.example.tmta.auth;

import com.example.tmta.auth.dto.AuthResponse;
import com.example.tmta.auth.dto.LoginRequest;
import com.example.tmta.auth.dto.PasswordResetResetRequest;
import com.example.tmta.auth.dto.SignUpRequest;
import com.example.tmta.auth.dto.SignUpResponse;
import com.example.tmta.auth.dto.SocialLoginRequest;
import com.example.tmta.auth.dto.WithdrawRequest;
import com.example.tmta.auth.social.SocialIdentity;
import com.example.tmta.auth.social.SocialVerifierRegistry;
import com.example.tmta.member.entity.Member;
import com.example.tmta.auth.entity.RefreshToken;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.example.tmta.appointment.repository.AvailableTimeRepository;
import com.example.tmta.member.repository.MemberRepository;
import com.example.tmta.auth.repository.RefreshTokenRepository;
import com.example.tmta.common.security.JwtProperties;
import com.example.tmta.common.security.JwtTokenProvider;
import com.example.tmta.common.security.UserPrincipal;
import com.example.tmta.member.entity.type.AuthProvider;
import com.example.tmta.team.repository.TeamMembersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

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
    private final SocialVerifierRegistry socialVerifierRegistry;
    private final TeamMembersRepository teamMembersRepository;
    private final AvailableTimeRepository availableTimeRepository;

    /** 이메일 인증 토큰이 검증된 사용자만 로컬 회원가입을 처리합니다. */
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        validateRequiredTermsAgreement(request);
        emailVerificationService.assertEmailVerifiedForSignUp(request.email(), request.verificationToken());

        String normalizedEmail = normalizeEmail(request.email());
        if (memberRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_DUPLICATION);
        }

        Member member = Member.registerLocal(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                Boolean.TRUE.equals(request.marketingAgreed())
        );

        // TODO(feature-social-login): Google/Naver OAuth 로그인 연동 및 providerId 계정 매핑을 구현합니다.
        // TODO(feature-email-verification): 이메일 인증 이력/정책(재시도 제한, 감사로그) 저장소를 외부 DB로 확장합니다.

        memberRepository.save(member);

        return new SignUpResponse(member.getId(), member.getEmail());
    }

    private void validateRequiredTermsAgreement(SignUpRequest request) {
        if (!Boolean.TRUE.equals(request.serviceTermsAgreed())
                || !Boolean.TRUE.equals(request.privacyPolicyAgreed())
                || !Boolean.TRUE.equals(request.ageOver14Agreed())) {
            throw new BusinessException(ErrorCode.REQUIRED_TERMS_AGREEMENT);
        }
    }

    /** 로컬 계정 인증 후 Access/Refresh 토큰을 발급합니다. */
    @Transactional
    public LoginResult login(LoginRequest request) {
        try {
            UserPrincipal principal = authenticate(request.email(), request.password());
            Member member = memberRepository.findById(principal.memberId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            return issueLoginResult(member);
        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    /** 소셜 토큰을 검증하고 회원가입/로그인을 처리합니다. */
    @Transactional
    public LoginResult socialLogin(SocialLoginRequest request) {
        SocialIdentity identity = socialVerifierRegistry.get(request.provider()).verify(request);
        Member member = memberRepository.findByAuthProviderAndProviderId(identity.provider(), identity.providerUserId())
                .orElseGet(() -> registerSocialMember(identity, Boolean.TRUE.equals(request.marketingAgreed())));

        return issueLoginResult(member);
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

    /** 비밀번호 재설정 인증번호를 발송합니다. (LOCAL 계정만 허용) */
    @Transactional(readOnly = true)
    public void sendPasswordResetCode(String email) {
        Member member = memberRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_NOT_ALLOWED);
        }
        emailVerificationService.sendPasswordResetCode(member.getEmail());
    }

    /** 비밀번호 재설정 토큰을 검증하고 비밀번호를 변경합니다. */
    @Transactional
    public void resetPassword(PasswordResetResetRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        emailVerificationService.assertEmailVerifiedForPasswordReset(normalizedEmail, request.resetToken());
        Member member = memberRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_NOT_ALLOWED);
        }
        member.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.deleteByMember(member);
    }

    /** 회원 탈퇴: 세션/참여 데이터 일부를 정리한 뒤 회원 레코드를 삭제합니다. */
    @Transactional
    public void withdraw(Long memberId, WithdrawRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getAuthProvider() == AuthProvider.LOCAL) {
            String password = request == null ? null : request.password();
            if (password == null || password.isBlank() || !passwordEncoder.matches(password, member.getPassword())) {
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
        }

        refreshTokenRepository.deleteByMember(member);
        availableTimeRepository.deleteAllByMemberId(memberId);
        teamMembersRepository.deleteAllByMemberId(memberId);
        memberRepository.delete(member);
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

    private LoginResult issueLoginResult(Member member) {
        UserPrincipal principal = new UserPrincipal(
                member.getId(),
                member.getEmail(),
                member.getPassword() == null ? "" : member.getPassword(),
                member.getRole().name()
        );

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
    }

    private Member registerSocialMember(SocialIdentity identity, boolean marketingAgreed) {
        String email = normalizeNullableEmail(identity.email());
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_EMAIL_REQUIRED);
        }

        memberRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.getAuthProvider() != identity.provider()
                    || existing.getProviderId() == null
                    || !existing.getProviderId().equals(identity.providerUserId())) {
                throw new BusinessException(ErrorCode.ACCOUNT_PROVIDER_MISMATCH);
            }
        });

        Member member = Member.registerSocial(email, identity.provider(), identity.providerUserId(), marketingAgreed);
        return memberRepository.save(member);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullableEmail(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
