package com.example.tmta.appointment;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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

import com.example.tmta.appointment.dto.AppointmentCandidateFilter;
import com.example.tmta.appointment.dto.AppointmentConfirmRequestDto;
import com.example.tmta.appointment.dto.AppointmentListResponseDto;
import com.example.tmta.appointment.dto.AppointmentDetailResponseDto;
import com.example.tmta.appointment.dto.AppointmentSaveRequestDto;
import com.example.tmta.appointment.dto.AppointmentTimeRegisterRequestDto;
import com.example.tmta.appointment.dto.AppointmentTimeTableResponseDto;
import com.example.tmta.appointment.dto.AppointmentUpdateRequestDto;
import com.example.tmta.appointment.AppointmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import lombok.RequiredArgsConstructor;

@Tag(name = "Appointment", description = "팀 약속 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams/{teamId}/appointments")
public class AppointmentController {
private final AppointmentService appointmentService;

	@Operation(summary = "팀 약속 생성")
	@PostMapping
	public ResponseEntity<Map<String, UUID>> createTeamAppointment(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @RequestBody AppointmentSaveRequestDto request){
		UUID appointmentId = appointmentService.createTeamAppointment(teamId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("appointmentId", appointmentId));
	}

	@Operation(summary = "팀 약속 상세 조회")
	@GetMapping("/{appointmentId}")
	public ResponseEntity<AppointmentDetailResponseDto> getTeamAppointmentDetail(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId){
		return ResponseEntity.ok(appointmentService.getTeamAppointmentDetail(teamId, appointmentId));
	}

	@Operation(summary = "팀 약속 후보 시간 조회")
	@GetMapping("/{appointmentId}/list")
	public ResponseEntity<AppointmentListResponseDto> getTeamAppointmentCandidate(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId,
		@ParameterObject @ModelAttribute AppointmentCandidateFilter urlRequest) {
		return ResponseEntity.ok(appointmentService.getTeamAppointmentCandidate(teamId, appointmentId, urlRequest));
	}


	@Operation(summary = "팀 약속 타임테이블 조회")
	@GetMapping("/{appointmentId}/timetable")
	public ResponseEntity<AppointmentTimeTableResponseDto> getTeamAppointmentTimeTable(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId,
		@ParameterObject @ModelAttribute AppointmentCandidateFilter urlRequest) {
		return ResponseEntity.ok(appointmentService.getTeamAppointmentTimeTable(teamId, appointmentId, urlRequest));
	}


	@Operation(summary = "팀 약속 마감")
	@PostMapping("/{appointmentId}/close")
	public ResponseEntity<Void> deadlineTeamAppointment(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId){
		appointmentService.deadlineTeamAppointment(teamId, appointmentId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "약속 가능 시간 등록")
	@PostMapping("/{appointmentId}")
	public ResponseEntity<Void> registerTeamAppointmentTime(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId, @RequestBody AppointmentTimeRegisterRequestDto request){
		appointmentService.registerTeamAppointmentTime(teamId, appointmentId, request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "팀 약속 확정")
	@PostMapping("/{appointmentId}/confirm")
	public ResponseEntity<Void> confirmTeamAppointment(@Parameter(description = "팀 ID") @PathVariable UUID teamId,
		@Parameter(description = "약속 ID") @PathVariable UUID appointmentId,
		@Valid @RequestBody AppointmentConfirmRequestDto request) {
		appointmentService.confirmTeamAppointment(teamId, appointmentId, request);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "팀 약속 삭제")
	@DeleteMapping("/{appointmentId}")
	public ResponseEntity<Void> deleteTeamAppointment(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId){
		appointmentService.deleteTeamAppointment(teamId, appointmentId);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "팀 약속 수정")
	@PutMapping("/{appointmentId}")
	public ResponseEntity<Void> updateTeamAppointment(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId,
		@RequestBody AppointmentUpdateRequestDto request) {
		appointmentService.updateTeamAppointment(teamId, appointmentId, request);
		return ResponseEntity.noContent().build();
	}

}
