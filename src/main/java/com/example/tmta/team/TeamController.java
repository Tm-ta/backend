package com.example.tmta.team;

import com.example.tmta.common.exception.dto.ErrorResponse;
import com.example.tmta.team.dto.DetailTeamList;
import com.example.tmta.team.dto.TeamListResponseDto;
import com.example.tmta.team.dto.TeamProfileSetupRequestDto;
import com.example.tmta.team.dto.TeamSaveRequestDto;
import com.example.tmta.team.dto.TeamSaveResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Team", description = "팀 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    @Operation(
            summary = "팀 목록 조회",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**

### 예외상황 / 에러코드
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 프로필 초기 설정 미완료.
- `MEMBER_NOT_FOUND (M001, 404)` : 회원 조회 실패.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팀 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다 (M001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<TeamListResponseDto> getTeamList(){
        return new ResponseEntity<>(teamService.getTeamList(), HttpStatus.OK);
    }

    @Operation(
            summary = "팀 생성",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- teamName은 blank일 수 없습니다. (trim 후 저장)

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 요청 필드 검증 실패 또는 teamName blank.
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 프로필 초기 설정 미완료.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "팀 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다 (C001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "회원 없음 (M001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<TeamSaveResponseDto> createTeam(@Valid @RequestBody TeamSaveRequestDto requestDto) {
        return new ResponseEntity<>(teamService.createTeam(requestDto), HttpStatus.CREATED);
    }

    @Operation(
            summary = "팀 상세 조회",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- 해당 팀 멤버만 조회 가능합니다.

### 예외상황 / 에러코드
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 팀 멤버가 아님.
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 프로필 초기 설정 미완료.
- `TEAM_NOT_FOUND (T001, 404)` : 팀 없음.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팀 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "팀 멤버가 아님 (T005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "팀 또는 사용자를 찾을 수 없습니다 (T001, M001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{teamId}")
    public ResponseEntity<DetailTeamList> getDetailTeam(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId){
        return new ResponseEntity<>(teamService.getDetailTeam(teamId), HttpStatus.OK);
    }

    @Operation(
            summary = "팀 가입",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- 이미 가입된 팀에는 중복 가입할 수 없습니다.

### 예외상황 / 에러코드
- `ALREADY_JOINED_TEAM (T004, 400)` : 이미 가입된 팀.
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 프로필 초기 설정 미완료.
- `TEAM_NOT_FOUND (T001, 404)` : 팀 없음.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팀 가입 성공"),
            @ApiResponse(responseCode = "400", description = "이미 가입된 팀입니다 (T004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 또는 팀을 찾을 수 없습니다 (M001, T001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/{teamId}")
    public ResponseEntity<?> joinTeam(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId){
        teamService.joinTeam(teamId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
            summary = "팀 프로필 설정/수정",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- teamNickName은 필수이며 최대 30자, teamProfileImage는 최대 255자입니다.
- 해당 팀 멤버만 설정할 수 있습니다.

### 예외상황 / 에러코드
- `INVALID_INPUT_VALUE (C001, 400)` : 요청 필드 검증 실패.
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 팀 멤버가 아님.
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 프로필 초기 설정 미완료.
- `TEAM_NOT_FOUND (T001, 404)` : 팀 없음.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "팀 프로필 설정/수정 성공"),
            @ApiResponse(responseCode = "400", description = "팀 멤버가 아님 (T005), 입력값 오류 (C001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 또는 팀을 찾을 수 없습니다 (M001, T001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{teamId}/me/profile")
    public ResponseEntity<?> setupTeamProfile(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId,
                                              @Valid @RequestBody TeamProfileSetupRequestDto request){
        teamService.setupMyTeamProfile(teamId, request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "팀 탈퇴",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- 팀장(ADMIN)은 탈퇴할 수 없습니다.

### 예외상황 / 에러코드
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 팀 멤버가 아님.
- `LEADER_CANNOT_EXIT_TEAM (T006, 400)` : 팀장은 탈퇴 불가.
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 프로필 초기 설정 미완료.
- `TEAM_NOT_FOUND (T001, 404)` : 팀 없음.
- `MEMBER_NOT_FOUND (M001, 404)` : 인증 주체 회원을 찾을 수 없는 경우.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "팀 탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "팀장은 팀을 나갈 수 없습니다 (T006), 팀 멤버가 아님 (T005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 또는 팀을 찾을 수 없습니다 (M001, T001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> exitTeam(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId){
        teamService.exitTeam(teamId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(
            summary = "팀장 위임",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **현재 요청 사용자가 팀장(ADMIN)이어야 합니다.**
- **대상 멤버는 해당 팀 멤버여야 합니다.**

### 예외상황 / 에러코드
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청자 또는 대상이 팀 멤버가 아님.
- `NOT_A_LEADER_OF_TEAM (T007, 400)` : 요청자가 팀장이 아님.
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 프로필 초기 설정 미완료.
- `TEAM_NOT_FOUND (T001, 404)` : 팀 없음.
- `MEMBER_NOT_FOUND (M001, 404)` : 대상 멤버 없음.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팀장 위임 성공"),
            @ApiResponse(responseCode = "400", description = "팀 리더가 아닙니다 (T007), 팀 멤버가 아님 (T005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 또는 팀을 찾을 수 없습니다 (M001, T001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{teamId}/member/{memberId}")
    public ResponseEntity<?> delegateLeader(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId, @Parameter(description = "멤버 ID") @PathVariable Long memberId){
        teamService.delegateLeader(teamId, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(
            summary = "팀원 강제 추방",
            description = """
### 제약조건
- **인증된 사용자만 호출할 수 있습니다.**
- **사용자 프로필 설정(profileSetupCompleted)이 완료되어야 합니다.**
- **현재 요청 사용자가 팀장(ADMIN)이어야 합니다.**
- 팀장은 본인을 강퇴할 수 없습니다.

### 예외상황 / 에러코드
- `NOT_A_MEMBER_OF_TEAM (T005, 400)` : 요청자 또는 대상이 팀 멤버가 아님.
- `NOT_A_LEADER_OF_TEAM (T007, 400)` : 요청자가 팀장이 아님.
- `CANNOT_KICK_LEADER (T008, 400)` : 팀장 강퇴 시도.
- `UNAUTHORIZED (C004, 401)` : 인증되지 않은 요청.
- `PROFILE_SETUP_REQUIRED (M005, 403)` : 프로필 초기 설정 미완료.
- `TEAM_NOT_FOUND (T001, 404)` : 팀 없음.
- `MEMBER_NOT_FOUND (M001, 404)` : 대상 멤버 없음.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "팀원 강제 추방 성공"),
            @ApiResponse(responseCode = "400", description = "팀 리더가 아님 (T007), 팀장 강퇴 불가 (T008), 팀 멤버가 아님 (T005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (C004)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "프로필 설정 필요 (M005)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "사용자 또는 팀을 찾을 수 없습니다 (M001, T001)",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{teamId}/member/{memberId}")
    public ResponseEntity<?> kickMember(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId, @Parameter(description = "멤버 ID") @PathVariable Long memberId){
        teamService.kickMember(teamId, memberId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
