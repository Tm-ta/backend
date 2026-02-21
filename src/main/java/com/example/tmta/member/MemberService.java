package com.example.tmta.member;

import com.example.tmta.member.entity.Member;
import com.example.tmta.member.dto.ProfileSetupRequest;
import com.example.tmta.common.security.CurrentMemberProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final CurrentMemberProvider currentMemberProvider;

    /** 현재 로그인 사용자의 프로필(닉네임/이미지)을 설정합니다. */
    @Transactional
    public void setupProfile(ProfileSetupRequest request) {
        Member member = currentMemberProvider.getCurrentMember();

        // 최초 설정 중 이탈한 사용자를 위해, profileSetupCompleted 플래그를 서버에서 관리합니다.
        // 이 API를 재호출하면 언제든 이어서 설정 완료할 수 있습니다.
        member.updateProfile(request.nickname(), request.profileImage());
    }
}
