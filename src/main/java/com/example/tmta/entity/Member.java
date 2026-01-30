package com.example.tmta.entity;

import com.example.tmta.entity.type.MemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String nickName;
    private String email;
    private String authProvider;
    private String providerId;
    private boolean pushAlarmAgree;
    @Enumerated(value = STRING)
    private MemberRole role;
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMembers> teamMembersList = new ArrayList<>();
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AvailableTime> availableTimeList = new ArrayList<>();
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vote> voteList = new ArrayList<>();

    @Builder
    public Member(String name, String nickName, String email, String authProvider, String providerId, boolean pushAlarmAgree, MemberRole role) {
        this.name = name;
        this.nickName = nickName;
        this.email = email;
        this.authProvider = authProvider;
        this.providerId = providerId;
        this.pushAlarmAgree = pushAlarmAgree;
        this.role = role;
    }
}