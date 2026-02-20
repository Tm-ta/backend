package com.example.tmta.service;

import com.example.tmta.exception.BusinessException;
import com.example.tmta.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class AppointmentCommandPolicy {

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
