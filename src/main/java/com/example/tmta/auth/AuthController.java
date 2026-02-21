package com.example.tmta.auth;

import com.example.tmta.auth.dto.AuthResponse;
import com.example.tmta.auth.dto.EmailVerificationConfirmRequest;
import com.example.tmta.auth.dto.EmailVerificationConfirmResponse;
import com.example.tmta.auth.dto.EmailVerificationSendRequest;
import com.example.tmta.auth.dto.LoginRequest;
import com.example.tmta.auth.dto.SignUpRequest;
import com.example.tmta.auth.dto.SignUpResponse;
import com.example.tmta.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @Operation(summary = "이메일 인증번호 발송", description = "회원가입을 위한 이메일 인증번호를 발송합니다.")
    @PostMapping("/email-verification/send")
    public ResponseEntity<Void> sendEmailVerificationCode(@Valid @RequestBody EmailVerificationSendRequest request) {
        emailVerificationService.sendVerificationCode(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "이메일 인증번호 확인", description = "발송된 인증번호를 검증합니다.")
    @PostMapping("/email-verification/confirm")
    public ResponseEntity<EmailVerificationConfirmResponse> confirmEmailVerificationCode(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        return ResponseEntity.ok(emailVerificationService.confirmVerificationCode(request));
    }

    @Operation(summary = "회원가입", description = "이메일/비밀번호 기반 회원가입")
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return new ResponseEntity<>(authService.signUp(request), HttpStatus.CREATED);
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호 로그인")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);
        attachRefreshCookie(response, result.refreshToken(), result.refreshTokenValiditySeconds());
        return ResponseEntity.ok(result.response());
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        AuthService.LoginResult result = authService.refresh(refreshToken);
        attachRefreshCookie(response, result.refreshToken(), result.refreshTokenValiditySeconds());
        return ResponseEntity.ok(result.response());
    }

    @Operation(summary = "로그아웃", description = "Refresh Token 삭제")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal,
                                       HttpServletResponse response) {
        authService.logout(principal.memberId());
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void attachRefreshCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
