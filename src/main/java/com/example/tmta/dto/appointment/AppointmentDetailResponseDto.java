package com.example.tmta.dto.appointment;

import java.time.LocalDate;
import java.util.List;

import com.example.tmta.entity.type.AppointmentState;

import lombok.Data;

@Data
public class AppointmentDetailResponseDto {
	private String name;
	private String organizerName;
	private Long memberCount;
	private LocalDate startDate;
	private LocalDate endDate;
	private List<?> member;
	private String description;
	// private String location;
	private AppointmentState state;
}
