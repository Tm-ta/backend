package com.example.tmta.dto.appointment;

import java.util.List;

public class AppointmentCandidateFilter {

	// 해당 유저가 참여 가능한 시간대
	private List<Long> userIds;

	// 해당 유저 수만큼 참여한 시간대
	private Long availableUserCount;

	// 해당 시간대만큼 가능한 시간대
	private Long availableTimeSlotCount;


}
