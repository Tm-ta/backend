package com.example.tmta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tmta.dto.appointment.AppointmentCandidateFilter;
import com.example.tmta.dto.appointment.AppointmentListResponseDto;
import com.example.tmta.dto.appointment.AppointmentDetailResponseDto;
import com.example.tmta.dto.appointment.AppointmentSaveRequestDto;
import com.example.tmta.dto.appointment.AppointmentTimeRegisterRequestDto;
import com.example.tmta.dto.appointment.AppointmentTimeTableResponseDto;
import com.example.tmta.dto.appointment.AppointmentUpdateRequestDto;
import com.example.tmta.service.AppointmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams/{teamId}/appointments")
public class AppointmentController {
private final AppointmentService appointmentService;

	// 팀 일정 생성
	@PostMapping
	public ResponseEntity<?> createTeamAppointment(@PathVariable Long teamId, @RequestBody AppointmentSaveRequestDto request){
		return ResponseEntity.ok().build();
	}

	// 팀 일정 상세 조회
	@GetMapping("/{appointmentId}")
	public ResponseEntity<AppointmentDetailResponseDto> getTeamAppointmentDetail(@PathVariable Long teamId, @PathVariable Long appointmentId){
		return ResponseEntity.ok().build();
	}

	// 시간 등록 마감 후 후보지 조회(필터링 {참여자가 있는 시간대, 참여 인원이 n명 이상인 시간대, 연속된 시간이 n시간 이상인 시간다}, 정렬{날짜 가까운 순, 참여자 많은 순, 연속된 시간이 긴 순})
	@GetMapping("/{appointmentId}/list")
	public ResponseEntity<AppointmentListResponseDto> getTeamAppointmentCandidate(@PathVariable Long teamId, @PathVariable Long appointmentId,
		@ModelAttribute AppointmentCandidateFilter urlRequest) {
		return ResponseEntity.ok().build();
	}


	@GetMapping("/{appointmentId}/timetable")
	public ResponseEntity<AppointmentTimeTableResponseDto> getTeamAppointmentTimeTable(@PathVariable Long teamId, @PathVariable Long appointmentId,
		@ModelAttribute AppointmentCandidateFilter urlRequest) {
		return ResponseEntity.ok().build();
	}


	// 팀 일정 마감
	@PostMapping("/{appointmentId}/close")
	public ResponseEntity<?> deadlineTeamAppointment(@PathVariable Long teamId, @PathVariable Long appointmentId){
		return ResponseEntity.ok().build();
	}

	// 약속 시간 등록
	@PostMapping("/{appointmentId}")
	public ResponseEntity<?> registerTeamAppointmentTime(@PathVariable Long teamId, @PathVariable Long appointmentId, @RequestBody AppointmentTimeRegisterRequestDto request){
		return ResponseEntity.ok().build();
	}

	// 약속 삭제 (해당 약속을 만든사람 or 그룹장만 가능)
	@DeleteMapping("/{appointmentId}")
	public ResponseEntity<?> deleteTeamAppointment(@PathVariable Long teamId, @PathVariable Long appointmentId){
		return ResponseEntity.ok().build();
	}

	// 팀 일정 수정
	@PutMapping("/{appointmentId}")
	public ResponseEntity<?> updateTeamAppointment(@PathVariable Long teamId, @PathVariable Long appointmentId,
		@RequestBody AppointmentUpdateRequestDto request) {
		return ResponseEntity.ok().build();
	}

}
