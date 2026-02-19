package com.example.tmta.dto.appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import lombok.Data;

@Data
public class AppointmentSaveRequestDto {
	private String name;
	private List<LocalDate> appointmentDates;
	// 날짜만 선택하는 경우 startTime과 endTime은 null로 설정
	private LocalTime startTime;;
	private LocalTime endTime;
	private boolean onlyDate;
	private String description;
	private LocalDateTime deadlineDateTime;
}
