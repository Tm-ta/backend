package com.example.tmta.service;

import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentCommandPolicyTest {

    private final AppointmentCommandPolicy policy = new AppointmentCommandPolicy();

    @Test
    @DisplayName("이름이 비어있으면 INVALID_INPUT_VALUE 예외")
    void invalidWhenNameBlank() {
        assertThatThrownBy(() -> policy.validateAppointmentCommand(
                "  ", List.of(LocalDate.now()), true, null, null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("날짜 목록이 비어있으면 INVALID_INPUT_VALUE 예외")
    void invalidWhenDatesEmpty() {
        assertThatThrownBy(() -> policy.validateAppointmentCommand(
                "회의", List.of(), true, null, null
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("onlyDate=false에서 endTime<=startTime이면 INVALID_INPUT_VALUE 예외")
    void invalidWhenEndNotAfterStart() {
        assertThatThrownBy(() -> policy.validateAppointmentCommand(
                "회의",
                List.of(LocalDate.now()),
                false,
                LocalTime.of(10, 0),
                LocalTime.of(10, 0)
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }
}
