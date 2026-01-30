package com.example.tmta.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", " 잘못된 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", " 지원하지 않는 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 내부 오류입니다."),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "해당 회원을 찾을 수 없습니다."),
    EMAIL_DUPLICATION(HttpStatus.CONFLICT, "M002", "이미 사용 중인 이메일입니다."),

    // Appointment
    APPOINTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "해당 약속을 찾을 수 없습니다."),

    // Team
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "해당 팀을 찾을 수 없습니다."),
    TEAM_NAME_DUPLICATION(HttpStatus.CONFLICT, "T002", "이미 사용 중인 팀 이름입니다."),
    TEAM_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "T003", "팀 최대 인원을 초과했습니다."),
    SELF_INVITE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "T004", "자기 자신을 초대할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
