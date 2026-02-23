package com.example.tmta.appointment;

import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class AppointmentCommandPolicy {

    /** 약속 생성/수정 요청의 필수 값과 시간 범위를 검증합니다. */
    public void validateAppointmentCommand(String name, List<LocalDate> dates, boolean onlyDate, LocalTime startTime, LocalTime endTime) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (dates == null || dates.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!onlyDate && startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
