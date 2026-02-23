package com.example.tmta.common.exception.dto;

import com.example.tmta.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "공통 에러 응답")
public class ErrorResponse {

    @Schema(description = "에러 코드", example = "C001")
    private String code;

    @Schema(description = "에러 메시지", example = "잘못된 입력 값입니다.")
    private String message;

    @Schema(description = "에러 발생 시각(UTC, ISO-8601)", example = "2026-02-21T10:15:30.000Z")
    private Instant timestamp;

    private ErrorResponse(ErrorCode code) {
        this.code = code.getCode();
        this.message = code.getMessage();
        this.timestamp = Instant.now();
    }

    private ErrorResponse(ErrorCode code, String message) {
        this.code = code.getCode();
        this.message = message;
        this.timestamp = Instant.now();
    }

    public static ErrorResponse of(ErrorCode code) {
        return new ErrorResponse(code);
    }

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code, message);
    }
}
