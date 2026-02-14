package com.example.tmta.controller;

import com.example.tmta.dto.team.*;
import com.example.tmta.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Operation(summary = "팀 목록 조회", description = "사용자가 속한 팀 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팀 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.")
    })
    @GetMapping
    public ResponseEntity<TeamListResponseDto> getTeamList(){
        return new ResponseEntity<>(teamService.getTeamList(), HttpStatus.OK);
    }

    @Operation(summary = "팀 생성", description = "새로운 팀을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "팀 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다.")
    })
    @PostMapping
    public ResponseEntity<TeamSaveResponseDto> createTeam(@Valid @RequestBody TeamSaveRequestDto requestDto) {
        return new ResponseEntity<>(teamService.createTeam(requestDto), HttpStatus.CREATED);
    }

    @Operation(summary = "팀 상세 조회", description = "특정 팀의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팀 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "팀을 찾을 수 없습니다.")
    })
    @GetMapping("/{teamId}")
    public ResponseEntity<DetailTeamList> getDetailTeam(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId){
        return new ResponseEntity<>(teamService.getDetailTeam(teamId), HttpStatus.OK);
    }


    @Operation(summary = "팀 가입", description = "사용자가 특정 팀에 가입합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팀 가입 성공"),
            @ApiResponse(responseCode = "400", description = "이미 가입된 팀입니다."),
            @ApiResponse(responseCode = "404", description = "사용자 또는 팀을 찾을 수 없습니다.")
    })
    @PostMapping("/{teamId}")
    public ResponseEntity<?> joinTeam(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId){
        teamService.joinTeam(teamId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "팀 프로필 설정/수정", description = "팀에서 사용할 닉네임/프로필 이미지를 설정하거나 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "팀 프로필 설정/수정 성공"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 팀을 찾을 수 없습니다.")
    })
    @PatchMapping("/{teamId}/me/profile")
    public ResponseEntity<?> setupTeamProfile(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId,
                                              @Valid @RequestBody TeamProfileSetupRequestDto request){
        teamService.setupMyTeamProfile(teamId, request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "팀 탈퇴", description = "사용자가 특정 팀에서 탈퇴합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "팀 탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "팀장은 팀을 나갈 수 없습니다."),
            @ApiResponse(responseCode = "404", description = "사용자 또는 팀을 찾을 수 없습니다.")
    })
    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> exitTeam(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId){
        teamService.exitTeam(teamId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "팀장 위임", description = "팀장이 다른 멤버에게 팀장 역할을 위임합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "팀장 위임 성공"),
            @ApiResponse(responseCode = "400", description = "팀의 리더가 아닙니다."),
            @ApiResponse(responseCode = "404", description = "사용자 또는 팀을 찾을 수 없습니다.")
    })
    @PostMapping("/{teamId}/member/{memberId}")
    public ResponseEntity<?> delegateLeader(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId, @Parameter(description = "멤버 ID") @PathVariable Long memberId){
        teamService.delegateLeader(teamId, memberId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "팀원 강제 추방", description = "팀장이 팀원을 강제로 추방합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "팀원 강제 추방 성공"),
            @ApiResponse(responseCode = "400", description = "팀의 리더가 아닙니다."),
            @ApiResponse(responseCode = "404", description = "사용자 또는 팀을 찾을 수 없습니다.")
    })
    @DeleteMapping("/{teamId}/member/{memberId}")
    public ResponseEntity<?> kickMember(@Parameter(description = "팀 ID") @PathVariable java.util.UUID teamId, @Parameter(description = "멤버 ID") @PathVariable Long memberId){
        teamService.kickMember(teamId, memberId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
