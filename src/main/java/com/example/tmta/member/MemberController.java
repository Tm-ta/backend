package com.example.tmta.member;

import com.example.tmta.member.dto.ProfileSetupRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "최초 프로필 설정", description = "닉네임/프로필 이미지 설정 후 profileSetupCompleted=true 처리")
    @PatchMapping("/me/profile-setup")
    public ResponseEntity<Void> setupProfile(@Valid @RequestBody ProfileSetupRequest request) {
        memberService.setupProfile(request);
        return ResponseEntity.noContent().build();
    }
}
