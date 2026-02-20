package com.example.tmta.auth;

import com.example.tmta.auth.dto.LoginRequest;
import com.example.tmta.auth.dto.SignUpRequest;
import com.example.tmta.entity.Member;
import com.example.tmta.entity.RefreshToken;
import com.example.tmta.entity.type.AuthProvider;
import com.example.tmta.entity.type.MemberRole;
import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import com.example.tmta.repository.MemberRepository;
import com.example.tmta.repository.RefreshTokenRepository;
import com.example.tmta.security.JwtProperties;
import com.example.tmta.security.JwtTokenProvider;
import com.example.tmta.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = member(1L, "user@test.com", "encoded", true);
    }

    @Nested
    @DisplayName("signUp")
    class SignUp {

        @Test
        @DisplayName("이메일 인증 완료 + 중복 없음이면 회원가입 성공")
        void signUpSuccess() {
            SignUpRequest request = new SignUpRequest("user@test.com", "Passw0rd!", "verification-token");

            when(memberRepository.findByEmail(request.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
            when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
                Member saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 100L);
                return saved;
            });

            var response = authService.signUp(request);

            verify(emailVerificationService).assertEmailVerifiedForSignUp(request.email(), request.verificationToken());
            assertThat(response.memberId()).isEqualTo(100L);
            assertThat(response.email()).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 EMAIL_DUPLICATION 예외")
        void signUpDuplicateEmail() {
            SignUpRequest request = new SignUpRequest("dup@test.com", "Passw0rd!", "verification-token");
            when(memberRepository.findByEmail(request.email())).thenReturn(Optional.of(member));

            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_DUPLICATION);

            verify(memberRepository, never()).save(any(Member.class));
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("인증 실패 시 INVALID_CREDENTIALS 예외")
        void loginBadCredentials() {
            LoginRequest request = new LoginRequest("user@test.com", "wrong");
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        }

        @Test
        @DisplayName("기존 refresh 토큰이 있으면 rotate 한다")
        void loginRotateExistingRefreshToken() {
            LoginRequest request = new LoginRequest("user@test.com", "Passw0rd!");
            UserPrincipal principal = new UserPrincipal(member.getId(), member.getEmail(), member.getPassword(), member.getRole().name());
            Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

            RefreshToken existingToken = RefreshToken.issue(member, "old-refresh", LocalDateTime.now().plusDays(1));
            LocalDateTime newExpiresAt = LocalDateTime.now().plusDays(14);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
            when(jwtTokenProvider.createAccessToken(principal)).thenReturn("new-access");
            when(jwtTokenProvider.createRefreshToken(principal)).thenReturn("new-refresh");
            when(jwtTokenProvider.getExpiration("new-refresh")).thenReturn(newExpiresAt);
            when(refreshTokenRepository.findByMember(member)).thenReturn(Optional.of(existingToken));
            when(jwtProperties.refreshTokenValiditySeconds()).thenReturn(1209600L);

            AuthService.LoginResult result = authService.login(request);

            assertThat(result.response().accessToken()).isEqualTo("new-access");
            assertThat(result.refreshToken()).isEqualTo("new-refresh");
            assertThat(existingToken.getToken()).isEqualTo("new-refresh");
            assertThat(existingToken.getExpiresAt()).isEqualTo(newExpiresAt);
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("기존 refresh 토큰이 없으면 신규 저장")
        void loginSaveNewRefreshToken() {
            LoginRequest request = new LoginRequest("user@test.com", "Passw0rd!");
            UserPrincipal principal = new UserPrincipal(member.getId(), member.getEmail(), member.getPassword(), member.getRole().name());
            Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            LocalDateTime newExpiresAt = LocalDateTime.now().plusDays(14);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
            when(jwtTokenProvider.createAccessToken(principal)).thenReturn("new-access");
            when(jwtTokenProvider.createRefreshToken(principal)).thenReturn("new-refresh");
            when(jwtTokenProvider.getExpiration("new-refresh")).thenReturn(newExpiresAt);
            when(refreshTokenRepository.findByMember(member)).thenReturn(Optional.empty());
            when(jwtProperties.refreshTokenValiditySeconds()).thenReturn(1209600L);

            AuthService.LoginResult result = authService.login(request);

            assertThat(result.response().accessToken()).isEqualTo("new-access");
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            assertThat(captor.getValue().getToken()).isEqualTo("new-refresh");
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("빈 refresh 토큰이면 INVALID_REFRESH_TOKEN 예외")
        void refreshBlankToken() {
            assertThatThrownBy(() -> authService.refresh(" "))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("JWT 검증 실패 시 INVALID_REFRESH_TOKEN 예외")
        void refreshValidateFail() {
            doThrow(new RuntimeException("invalid")).when(jwtTokenProvider).validateToken(anyString());

            assertThatThrownBy(() -> authService.refresh("bad-token"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("저장소에 토큰이 없으면 INVALID_REFRESH_TOKEN 예외")
        void refreshNotFound() {
            when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh("refresh-token"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("만료된 refresh 토큰이면 삭제 후 INVALID_REFRESH_TOKEN 예외")
        void refreshExpiredToken() {
            RefreshToken expired = RefreshToken.issue(member, "refresh-token", LocalDateTime.now().minusMinutes(1));
            when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> authService.refresh("refresh-token"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

            verify(refreshTokenRepository).delete(expired);
        }

        @Test
        @DisplayName("유효한 refresh 토큰이면 access/refresh 재발급")
        void refreshSuccess() {
            RefreshToken stored = RefreshToken.issue(member, "refresh-token", LocalDateTime.now().plusDays(1));
            UserPrincipal principal = new UserPrincipal(member.getId(), member.getEmail(), member.getPassword(), member.getRole().name());
            LocalDateTime newExpiresAt = LocalDateTime.now().plusDays(14);

            when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(stored));
            when(jwtTokenProvider.createAccessToken(any(UserPrincipal.class))).thenReturn("new-access");
            when(jwtTokenProvider.createRefreshToken(any(UserPrincipal.class))).thenReturn("new-refresh");
            when(jwtTokenProvider.getExpiration("new-refresh")).thenReturn(newExpiresAt);
            when(jwtProperties.refreshTokenValiditySeconds()).thenReturn(1209600L);

            AuthService.LoginResult result = authService.refresh("refresh-token");

            assertThat(result.response().accessToken()).isEqualTo("new-access");
            assertThat(result.refreshToken()).isEqualTo("new-refresh");
            assertThat(stored.getToken()).isEqualTo("new-refresh");
            assertThat(stored.getExpiresAt()).isEqualTo(newExpiresAt);

            verify(jwtTokenProvider).createAccessToken(principal);
            verify(jwtTokenProvider).createRefreshToken(principal);
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("회원이 없으면 MEMBER_NOT_FOUND 예외")
        void logoutMemberNotFound() {
            when(memberRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.logout(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("회원이 있으면 해당 refresh 토큰 삭제")
        void logoutSuccess() {
            when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

            authService.logout(member.getId());

            verify(refreshTokenRepository).deleteByMember(member);
        }
    }

    private static Member member(Long id, String email, String password, boolean profileSetupCompleted) {
        return Member.builder()
                .id(id)
                .email(email)
                .password(password)
                .authProvider(AuthProvider.LOCAL)
                .role(MemberRole.GENERAL)
                .profileSetupCompleted(profileSetupCompleted)
                .emailVerified(true)
                .build();
    }
}
