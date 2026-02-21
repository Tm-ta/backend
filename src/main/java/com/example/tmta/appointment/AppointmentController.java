package com.example.tmta.appointment;

import java.util.Map;
import java.util.UUID;

import com.example.tmta.common.exception.dto.ErrorResponse;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

	@Operation(
			summary = "팀 약속 생성",
			description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **요청 사용자는 해당 팀 멤버여야 합니다.**
- name은 blank일 수 없고, appointmentDates는 비어있을 수 없습니다.
- onlyDate=false인 경우 endTime은 startTime보다 늦어야 합니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 요청 필드 또는 파라미터 검증에 실패한 경우.
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청 사용자가 해당 팀 멤버가 아닌 경우.
- `UNAUTHORIZED (C004, 401)` : 인증 정보가 없거나 유효하지 않은 경우.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 사용자 프로필 초기 설정이 완료되지 않은 경우.
- `TEAM_NOT_FOUND (T001, 404)` : 요청한 팀을 찾을 수 없는 경우.
					"""
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "생성 성공"),
			@ApiResponse(responseCode = "400", description = "입력 오류/팀 멤버 아님 (C001, T005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "팀/회원 없음 (T001, M001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping
	public ResponseEntity<Map<String, UUID>> createTeamAppointment(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @RequestBody AppointmentSaveRequestDto request){
		UUID appointmentId = appointmentService.createTeamAppointment(teamId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("appointmentId", appointmentId));
	}

	@Operation(
			summary = "팀 약속 상세 조회",
			description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **요청 사용자는 해당 팀 멤버여야 합니다.**

### 예외상황 / 에러코드
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청 사용자가 해당 팀 멤버가 아닌 경우.
- `UNAUTHORIZED (C004, 401)` : 인증 정보가 없거나 유효하지 않은 경우.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 사용자 프로필 초기 설정이 완료되지 않은 경우.
- `TEAM_NOT_FOUND (T001, 404)` : 요청한 팀을 찾을 수 없는 경우.
- `APPOINTMENT_NOT_FOUND (A001, 404)` : 요청한 약속을 찾을 수 없는 경우.
					"""
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "400", description = "팀 멤버 아님 (T005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "팀/약속/회원 없음 (T001, A001, M001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping("/{appointmentId}")
	public ResponseEntity<AppointmentDetailResponseDto> getTeamAppointmentDetail(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId){
		return ResponseEntity.ok(appointmentService.getTeamAppointmentDetail(teamId, appointmentId));
	}

	@Operation(
			summary = "팀 약속 후보 시간 조회",
			description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **요청 사용자는 해당 팀 멤버여야 합니다.**
- 필터 파라미터 바인딩 검증 실패 시 400이 반환됩니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 요청 필드 또는 파라미터 검증에 실패한 경우.
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청 사용자가 해당 팀 멤버가 아닌 경우.
- `UNAUTHORIZED (C004, 401)` : 인증 정보가 없거나 유효하지 않은 경우.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 사용자 프로필 초기 설정이 완료되지 않은 경우.
- `TEAM_NOT_FOUND (T001, 404)` : 요청한 팀을 찾을 수 없는 경우.
- `APPOINTMENT_NOT_FOUND (A001, 404)` : 요청한 약속을 찾을 수 없는 경우.
					"""
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "400", description = "팀 멤버 아님/필터 바인딩 오류 (T005, C001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "팀/약속/회원 없음 (T001, A001, M001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping("/{appointmentId}/list")
	public ResponseEntity<AppointmentListResponseDto> getTeamAppointmentCandidate(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId,
		@ParameterObject @ModelAttribute AppointmentCandidateFilter urlRequest) {
		return ResponseEntity.ok(appointmentService.getTeamAppointmentCandidate(teamId, appointmentId, urlRequest));
	}

	@Operation(
			summary = "팀 약속 타임테이블 조회",
			description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **요청 사용자는 해당 팀 멤버여야 합니다.**
- 필터 파라미터 바인딩 검증 실패 시 400이 반환됩니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 요청 필드 또는 파라미터 검증에 실패한 경우.
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청 사용자가 해당 팀 멤버가 아닌 경우.
- `UNAUTHORIZED (C004, 401)` : 인증 정보가 없거나 유효하지 않은 경우.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 사용자 프로필 초기 설정이 완료되지 않은 경우.
- `TEAM_NOT_FOUND (T001, 404)` : 요청한 팀을 찾을 수 없는 경우.
- `APPOINTMENT_NOT_FOUND (A001, 404)` : 요청한 약속을 찾을 수 없는 경우.
					"""
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "조회 성공"),
			@ApiResponse(responseCode = "400", description = "팀 멤버 아님/필터 바인딩 오류 (T005, C001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "팀/약속/회원 없음 (T001, A001, M001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@GetMapping("/{appointmentId}/timetable")
	public ResponseEntity<AppointmentTimeTableResponseDto> getTeamAppointmentTimeTable(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId,
		@ParameterObject @ModelAttribute AppointmentCandidateFilter urlRequest) {
		return ResponseEntity.ok(appointmentService.getTeamAppointmentTimeTable(teamId, appointmentId, urlRequest));
	}

	@Operation(
			summary = "팀 약속 마감",
			description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **요청 사용자는 팀장(ADMIN) 또는 약속 생성자여야 합니다.**
- **약속 상태가 SCHEDULING/CREATING 일 때만 마감 가능합니다.**

### 예외상황 / 에러코드
- `FORBIDDEN (C005, 403)` : 약속 관리 권한 없음.
- `INVALID_APPOINTMENT_STATE (A002, 400)` : 현재 약속 상태에서 수행할 수 없는 요청인 경우.
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청 사용자가 해당 팀 멤버가 아닌 경우.
- `UNAUTHORIZED (C004, 401)` : 인증 정보가 없거나 유효하지 않은 경우.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 사용자 프로필 초기 설정이 완료되지 않은 경우.
- `TEAM_NOT_FOUND (T001, 404)` : 요청한 팀을 찾을 수 없는 경우.
- `APPOINTMENT_NOT_FOUND (A001, 404)` : 요청한 약속을 찾을 수 없는 경우.
					"""
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "마감 성공"),
			@ApiResponse(responseCode = "400", description = "상태 오류/멤버 아님 (A002, T005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "권한 없음/프로필 설정 필요 (C005, M005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "팀/약속/회원 없음 (T001, A001, M001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/{appointmentId}/close")
	public ResponseEntity<Void> deadlineTeamAppointment(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId){
		appointmentService.deadlineTeamAppointment(teamId, appointmentId);
		return ResponseEntity.noContent().build();
	}

	@Operation(
			summary = "약속 가능 시간 등록",
			description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **요청 사용자는 해당 팀 멤버여야 합니다.**
- timeSlots는 비어있을 수 없습니다.
- 약속 날짜에 포함되지 않은 date는 등록할 수 없습니다.
- 약속 시간 범위를 벗어난 시간은 등록할 수 없습니다.
- **약속 상태가 SCHEDULING/CREATING 일 때만 등록 가능합니다.**

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 요청 필드 또는 파라미터 검증에 실패한 경우.
- `INVALID_APPOINTMENT_STATE (A002, 400)` : 현재 약속 상태에서 수행할 수 없는 요청인 경우.
- `APPOINTMENT_DATE_MISMATCH (A003, 400)` : 요청 날짜가 약속 후보 날짜와 일치하지 않는 경우.
- `APPOINTMENT_TIME_OUT_OF_RANGE (A004, 400)` : 요청 시간이 약속 허용 시간 범위를 벗어난 경우.
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청 사용자가 해당 팀 멤버가 아닌 경우.
- `UNAUTHORIZED (C004, 401)` : 인증 정보가 없거나 유효하지 않은 경우.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 사용자 프로필 초기 설정이 완료되지 않은 경우.
- `TEAM_NOT_FOUND (T001, 404)` : 요청한 팀을 찾을 수 없는 경우.
- `APPOINTMENT_NOT_FOUND (A001, 404)` : 요청한 약속을 찾을 수 없는 경우.
					"""
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "등록 성공"),
			@ApiResponse(responseCode = "400", description = "입력/상태/날짜-시간 범위 오류 (C001, A002, A003, A004, T005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "팀/약속/회원 없음 (T001, A001, M001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/{appointmentId}")
	public ResponseEntity<Void> registerTeamAppointmentTime(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId, @RequestBody AppointmentTimeRegisterRequestDto request){
		appointmentService.registerTeamAppointmentTime(teamId, appointmentId, request);
		return ResponseEntity.noContent().build();
	}

	@Operation(
			summary = "팀 약속 확정",
			description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **요청 사용자는 팀장(ADMIN) 또는 약속 생성자여야 합니다.**
- date는 약속 후보 날짜에 포함되어야 합니다.
- onlyDate=false인 경우 startTime/endTime이 필수이며 endTime > startTime 이어야 합니다.
- 확정 시간은 약속 허용 시간 범위 내여야 합니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 요청 필드 또는 파라미터 검증에 실패한 경우.
- `FORBIDDEN (C005, 403)` : 약속 관리 권한 없음.
- `INVALID_APPOINTMENT_STATE (A002, 400)` : 현재 약속 상태에서 수행할 수 없는 요청인 경우.
- `APPOINTMENT_DATE_MISMATCH (A003, 400)` : 요청 날짜가 약속 후보 날짜와 일치하지 않는 경우.
- `APPOINTMENT_TIME_OUT_OF_RANGE (A004, 400)` : 요청 시간이 약속 허용 시간 범위를 벗어난 경우.
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청 사용자가 해당 팀 멤버가 아닌 경우.
- `UNAUTHORIZED (C004, 401)` : 인증 정보가 없거나 유효하지 않은 경우.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 사용자 프로필 초기 설정이 완료되지 않은 경우.
- `TEAM_NOT_FOUND (T001, 404)` : 요청한 팀을 찾을 수 없는 경우.
- `APPOINTMENT_NOT_FOUND (A001, 404)` : 요청한 약속을 찾을 수 없는 경우.
					"""
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "확정 성공"),
			@ApiResponse(responseCode = "400", description = "입력/상태/날짜-시간 범위 오류 (C001, A002, A003, A004, T005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "권한 없음/프로필 설정 필요 (C005, M005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "팀/약속/회원 없음 (T001, A001, M001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PostMapping("/{appointmentId}/confirm")
	public ResponseEntity<Void> confirmTeamAppointment(@Parameter(description = "팀 ID") @PathVariable UUID teamId,
		@Parameter(description = "약속 ID") @PathVariable UUID appointmentId,
		@Valid @RequestBody AppointmentConfirmRequestDto request) {
		appointmentService.confirmTeamAppointment(teamId, appointmentId, request);
		return ResponseEntity.noContent().build();
	}

	@Operation(
			summary = "팀 약속 삭제",
			description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **요청 사용자는 팀장(ADMIN) 또는 약속 생성자여야 합니다.**

### 예외상황 / 에러코드
- `FORBIDDEN (C005, 403)` : 약속 관리 권한 없음.
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청 사용자가 해당 팀 멤버가 아닌 경우.
- `UNAUTHORIZED (C004, 401)` : 인증 정보가 없거나 유효하지 않은 경우.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 사용자 프로필 초기 설정이 완료되지 않은 경우.
- `TEAM_NOT_FOUND (T001, 404)` : 요청한 팀을 찾을 수 없는 경우.
- `APPOINTMENT_NOT_FOUND (A001, 404)` : 요청한 약속을 찾을 수 없는 경우.
					"""
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "삭제 성공"),
			@ApiResponse(responseCode = "400", description = "멤버 아님 (T005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "권한 없음/프로필 설정 필요 (C005, M005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "팀/약속/회원 없음 (T001, A001, M001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@DeleteMapping("/{appointmentId}")
	public ResponseEntity<Void> deleteTeamAppointment(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId){
		appointmentService.deleteTeamAppointment(teamId, appointmentId);
		return ResponseEntity.noContent().build();
	}

	@Operation(
			summary = "팀 약속 수정",
			description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **요청 사용자는 팀장(ADMIN) 또는 약속 생성자여야 합니다.**
- name은 blank일 수 없고, appointmentDates는 비어있을 수 없습니다.
- onlyDate=false인 경우 endTime > startTime 이어야 합니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 요청 필드 또는 파라미터 검증에 실패한 경우.
- `FORBIDDEN (C005, 403)` : 약속 관리 권한 없음.
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청 사용자가 해당 팀 멤버가 아닌 경우.
- `UNAUTHORIZED (C004, 401)` : 인증 정보가 없거나 유효하지 않은 경우.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 사용자 프로필 초기 설정이 완료되지 않은 경우.
- `TEAM_NOT_FOUND (T001, 404)` : 요청한 팀을 찾을 수 없는 경우.
- `APPOINTMENT_NOT_FOUND (A001, 404)` : 요청한 약속을 찾을 수 없는 경우.
					"""
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "수정 성공"),
			@ApiResponse(responseCode = "400", description = "입력 오류/멤버 아님 (C001, T005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "403", description = "권한 없음/프로필 설정 필요 (C005, M005)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "팀/약속/회원 없음 (T001, A001, M001)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
	})
	@PutMapping("/{appointmentId}")
	public ResponseEntity<Void> updateTeamAppointment(@Parameter(description = "팀 ID") @PathVariable UUID teamId, @Parameter(description = "약속 ID") @PathVariable UUID appointmentId,
		@RequestBody AppointmentUpdateRequestDto request) {
		appointmentService.updateTeamAppointment(teamId, appointmentId, request);
		return ResponseEntity.noContent().build();
	}

}
