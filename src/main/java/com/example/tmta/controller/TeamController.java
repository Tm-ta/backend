package com.example.tmta.controller;

import com.example.tmta.dto.team.*;
import com.example.tmta.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamSaveResponseDto> createTeam(@Valid @RequestBody TeamSaveRequestDto requestDto) {
        // TODO: Get memberId from security context
        Long memberId = 1L;
        return ResponseEntity.ok(teamService.createTeam(requestDto, memberId));
    }

    @PostMapping("/test-member")
    public ResponseEntity<com.example.tmta.entity.Member> createTestMember() {
        return ResponseEntity.ok(teamService.createTestMember());
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<DetailTeamList> getDetailTeam(@PathVariable java.util.UUID teamId){
        return ResponseEntity.ok(teamService.getDetailTeam(teamId));
    }

    @GetMapping
    public ResponseEntity<TeamListResponseDto> getTeamList(){
        return ResponseEntity.ok(teamService.getTeamList());
    }

    @PostMapping("/{teamId}")
    public ResponseEntity<?> joinTeam(@PathVariable java.util.UUID teamId, @RequestBody RequestJoinDto request){
        return ResponseEntity.ok(teamService.joinTeam(teamId, request));
    }

    @DeleteMapping("/{teamId}/member/{memberId}")
    public ResponseEntity<?> kickMember(@PathVariable java.util.UUID teamId, @PathVariable Long memberId){
        // TODO: Get leaderId from security context
        Long leaderId = 1L;
        teamService.kickMember(teamId, memberId, leaderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/member/{memberId}")
    public ResponseEntity<?> delegateLeader(@PathVariable java.util.UUID teamId, @PathVariable Long memberId){
        //TODO : 로그인 기능 구현 후 현재 로그인 된 사용자에서 memberId를 가진 사용자에게 그룹장 권한 위임
        Long leaderId = 1L;
        teamService.delegateLeader(teamId, memberId, leaderId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<?> exitTeam(@PathVariable java.util.UUID teamId){
        // TODO: Get memberId from security context
        Long memberId = 1L;
        teamService.exitTeam(teamId, memberId);
        return ResponseEntity.noContent().build();
    }
}
