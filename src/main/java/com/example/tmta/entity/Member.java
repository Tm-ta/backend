package com.example.tmta.entity;

import com.example.tmta.entity.type.MemberRole;
import com.example.tmta.entity.type.AuthProvider;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    private String profileImage;
    private boolean profileSetupCompleted;
    private boolean emailVerified;
    // TODO(email-verification): 아래 필드는 이메일 인증 기능 활성화 시 사용하세요.
    // private String emailVerificationToken;
    // private LocalDateTime emailVerificationExpiresAt;
    private boolean pushAlarmAgree;
    @Enumerated(value = STRING)
    private MemberRole role;
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Added Builder.Default
    private List<TeamMembers> teamMembersList = new ArrayList<>();
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Added Builder.Default
    private List<AvailableTime> availableTimeList = new ArrayList<>();
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Added Builder.Default
    private List<Vote> voteList = new ArrayList<>();

    public void updateProfile(String nickName, String profileImage) {
        this.nickName = nickName;
        this.profileImage = profileImage;
        this.profileSetupCompleted = true;
    }
}
