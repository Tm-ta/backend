package com.example.tmta.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", " 지원하지 않는 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C004", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C005", "접근 권한이 없습니다."),
    TERMS_NOT_FOUND(HttpStatus.NOT_FOUND, "C006", "해당 약관을 찾을 수 없습니다."),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "해당 회원을 찾을 수 없습니다."),
    EMAIL_DUPLICATION(HttpStatus.CONFLICT, "M002", "이미 사용 중인 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "M003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_AUTH_PROVIDER(HttpStatus.BAD_REQUEST, "M004", "해당 회원은 이메일/비밀번호 로그인을 사용할 수 없습니다."),
    PROFILE_SETUP_REQUIRED(HttpStatus.FORBIDDEN, "M005", "프로필 초기 설정이 필요합니다."),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST, "M006", "이메일 인증이 필요합니다."),
    EMAIL_VERIFICATION_MISMATCH(HttpStatus.BAD_REQUEST, "M007", "인증번호가 올바르지 않습니다."),
    EMAIL_VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "M008", "인증번호가 만료되었습니다."),
    EMAIL_VERIFICATION_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "M009", "유효하지 않은 이메일 인증 토큰입니다."),
    REQUIRED_TERMS_AGREEMENT(HttpStatus.BAD_REQUEST, "M010", "필수 약관 동의가 필요합니다."),

    // Appointment
    APPOINTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "해당 약속을 찾을 수 없습니다."),
    INVALID_APPOINTMENT_STATE(HttpStatus.BAD_REQUEST, "A002", "현재 약속 상태에서는 요청을 처리할 수 없습니다."),
    APPOINTMENT_DATE_MISMATCH(HttpStatus.BAD_REQUEST, "A003", "약속에 포함되지 않은 날짜가 포함되어 있습니다."),
    APPOINTMENT_TIME_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "A004", "약속 시간 범위를 벗어난 시간이 포함되어 있습니다."),

    // Team
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "해당 팀을 찾을 수 없습니다."),
    TEAM_NAME_DUPLICATION(HttpStatus.CONFLICT, "T002", "이미 사용 중인 팀 이름입니다."),
    TEAM_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "T003", "팀 최대 인원을 초과했습니다."),
    ALREADY_JOINED_TEAM(HttpStatus.BAD_REQUEST, "T004", "이미 가입된 팀입니다."),
    NOT_A_MEMBER_OF_TEAM(HttpStatus.BAD_REQUEST, "T005", "팀의 멤버가 아닙니다."),
    LEADER_CANNOT_EXIT_TEAM(HttpStatus.BAD_REQUEST, "T006", "팀장은 팀을 나갈 수 없습니다."),
    NOT_A_LEADER_OF_TEAM(HttpStatus.BAD_REQUEST, "T007", "팀의 리더가 아닙니다."),
    CANNOT_KICK_LEADER(HttpStatus.BAD_REQUEST, "T008", "팀장은 강퇴할 수 없습니다."),

    // Auth
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AU001", "유효하지 않은 리프레시 토큰입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
