package com.example.tmta.member.entity;
import com.example.tmta.common.entity.BaseEntity;

import com.example.tmta.member.entity.type.MemberRole;
import com.example.tmta.member.entity.type.AuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String nickName;
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    // LOCAL 계정에서는 필수, 소셜 계정(GOOGLE/NAVER)에서는 null 허용 예정입니다.
    private String password;
    @Enumerated(value = STRING)
    @Column(nullable = false)
    private AuthProvider authProvider;
    private String providerId;
    private String profileImageBucket;
    private String profileImageKey;
    private boolean profileSetupCompleted;
    private boolean emailVerified;
    private boolean pushAlarmAgree;
    @Enumerated(value = STRING)
    private MemberRole role;

    public static Member registerLocal(String email, String encodedPassword, boolean marketingAgreed) {
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .authProvider(AuthProvider.LOCAL)
                .role(MemberRole.GENERAL)
                .emailVerified(true)
                .profileSetupCompleted(false)
                .pushAlarmAgree(marketingAgreed)
                .build();
    }

    public static Member registerSocial(String email,
                                        AuthProvider authProvider,
                                        String providerId,
                                        boolean marketingAgreed) {
        return Member.builder()
                .email(email)
                .password(null)
                .authProvider(authProvider)
                .providerId(providerId)
                .role(MemberRole.GENERAL)
                .emailVerified(true)
                .profileSetupCompleted(false)
                .pushAlarmAgree(marketingAgreed)
                .build();
    }

    public void updateProfile(String nickName, String profileImageBucket, String profileImageKey) {
        this.nickName = nickName;
        this.profileImageBucket = profileImageBucket;
        this.profileImageKey = profileImageKey;
        this.profileSetupCompleted = true;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
