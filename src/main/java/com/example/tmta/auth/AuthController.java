package com.example.tmta.auth;

import com.example.tmta.auth.dto.AuthResponse;
import com.example.tmta.auth.dto.EmailVerificationConfirmRequest;
import com.example.tmta.auth.dto.EmailVerificationConfirmResponse;
import com.example.tmta.auth.dto.EmailVerificationSendRequest;
import com.example.tmta.auth.dto.LoginRequest;
import com.example.tmta.auth.dto.PasswordResetResetRequest;
import com.example.tmta.auth.dto.SignUpRequest;
import com.example.tmta.auth.dto.SignUpResponse;
import com.example.tmta.auth.dto.SocialLoginRequest;
import com.example.tmta.auth.dto.WithdrawRequest;
import com.example.tmta.common.exception.dto.ErrorResponse;
import com.example.tmta.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "이메일 인증번호 발송",
            description = """
### 제약조건
- 이메일은 trim + 소문자로 정규화되어 처리됩니다.
- **동일 이메일로 재요청 시 가장 마지막으로 저장된 인증번호만 유효합니다.**
- **인증번호 유효시간은 5분입니다.**
- 현재 구현의 인증번호는 고정값(1111)입니다.
- 현재 구현은 가입된 이메일 중복 여부를 검사하지 않습니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 이메일이 null/blank 이거나 DTO 검증에 실패한 경우.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "발송 성공"),
            @ApiResponse(responseCode = "400", description = "`INVALID_INPUT_VALUE (C001)`",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/email-verification/send")
    public ResponseEntity<Void> sendEmailVerificationCode(@Valid @RequestBody EmailVerificationSendRequest request) {
        emailVerificationService.sendVerificationCode(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "이메일 인증번호 확인",
            description = """
### 제약조건
- 이메일은 trim + 소문자로 정규화되어 비교됩니다.
- 발송 이력이 없는 이메일은 인증할 수 없습니다.
- **인증번호 유효시간(5분) 내의 코드만 검증 가능합니다.**
- **검증 성공 시 회원가입용 verificationToken(JWT)을 발급합니다.**

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 이메일/인증번호 형식 또는 필수값 오류.
- `EMAIL_VERIFICATION_REQUIRED (M006, 400)` : 발송 이력이 없거나 이메일이 불일치.
- `EMAIL_VERIFICATION_MISMATCH (M007, 400)` : 인증번호 불일치.
- `EMAIL_VERIFICATION_EXPIRED (M008, 400)` : 인증번호 만료.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "입력/인증 오류 (C001, M006, M007, M008)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/email-verification/confirm")
    public ResponseEntity<EmailVerificationConfirmResponse> confirmEmailVerificationCode(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        return ResponseEntity.ok(emailVerificationService.confirmVerificationCode(request));
    }

    @Operation(
            summary = "회원가입",
            description = """
### 제약조건
- **회원가입 전 이메일 인증 확인 API에서 발급한 verificationToken이 필요합니다.**
- 서비스 이용약관, 개인정보 처리방침, 만 14세 이상 동의는 모두 true여야 합니다.
- 마케팅 정보 수신 동의는 선택이며 false/null이어도 회원가입이 가능합니다.
- 비밀번호는 공백 없이 8자 이상이며 영문/숫자를 각각 1개 이상 포함해야 합니다.
- 동일 문자/숫자를 3회 이상 연속으로 사용할 수 없습니다. (예: aaa, 111)
- 동일 이메일은 중복 가입할 수 없습니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 요청 필드 검증 실패(비밀번호 정책 위반 등).
- `REQUIRED_TERMS_AGREEMENT (M010, 400)` : 필수 약관(서비스/개인정보/만 14세 이상) 중 하나라도 미동의.
- `EMAIL_VERIFICATION_REQUIRED (M006, 400)` : 이메일 인증 미완료 또는 이메일-토큰 불일치.
- `EMAIL_VERIFICATION_TOKEN_INVALID (M009, 400)` : 유효하지 않은 verificationToken.
- `EMAIL_DUPLICATION (M002, 409)` : 이미 가입된 이메일.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "입력/약관/인증 오류 (C001, M010, M006, M009)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이메일 중복 (M002)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return new ResponseEntity<>(authService.signUp(request), HttpStatus.CREATED);
    }

    @Operation(
            summary = "로그인",
            description = """
### 제약조건
- **LOCAL 계정만 이메일/비밀번호 로그인이 가능합니다.**
- 로그인 성공 시 기존 Refresh Token은 회전(rotate)되거나 신규 발급됩니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 이메일/비밀번호 형식 또는 필수값 오류.
- `INVALID_CREDENTIALS (M003, 401)` : 이메일 또는 비밀번호 불일치.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원 조회 실패.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 오류 (C001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (M003)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "회원 없음 (M001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthService.LoginResult result = authService.login(request);
        attachRefreshCookie(response, result.refreshToken(), result.refreshTokenValiditySeconds());
        return ResponseEntity.ok(result.response());
    }

    @PostMapping("/social/login")
    public ResponseEntity<AuthResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request,
                                                    HttpServletResponse response) {
        AuthService.LoginResult result = authService.socialLogin(request);
        attachRefreshCookie(response, result.refreshToken(), result.refreshTokenValiditySeconds());
        return ResponseEntity.ok(result.response());
    }

    @Operation(
            summary = "토큰 재발급",
            description = """
### 제약조건
- **요청 쿠키(refresh_token)에 유효한 Refresh Token이 필요합니다.**
- DB에 저장된 토큰과 일치해야 하며 만료되지 않아야 합니다.
- 재발급 시 Refresh Token은 회전(rotate)됩니다.

### 예외상황 / 에러코드
- `INVALID_REFRESH_TOKEN (AU001, 401)` : 누락/위조/만료/저장소 불일치 Refresh Token.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰 (AU001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getRefreshTokenFromCookie(request);
        AuthService.LoginResult result = authService.refresh(refreshToken);
        attachRefreshCookie(response, result.refreshToken(), result.refreshTokenValiditySeconds());
        return ResponseEntity.ok(result.response());
    }

    @Operation(
            summary = "로그아웃",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**

### 예외상황 / 에러코드
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `MEMBER_NOT_FOUND (M001, 404)` : 회원 조회 실패.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "회원 없음 (M001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal,
                                       HttpServletResponse response) {
        authService.logout(principal.memberId());
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/send")
    public ResponseEntity<Void> sendPasswordResetCode(@Valid @RequestBody EmailVerificationSendRequest request) {
        authService.sendPasswordResetCode(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<EmailVerificationConfirmResponse> confirmPasswordResetCode(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        return ResponseEntity.ok(emailVerificationService.confirmPasswordResetCode(request));
    }

    @PostMapping("/password-reset/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestBody(required = false) WithdrawRequest request,
                                         HttpServletResponse response) {
        authService.withdraw(principal.memberId(), request);
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
