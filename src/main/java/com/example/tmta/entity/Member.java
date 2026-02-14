package com.example.tmta.entity;

import com.example.tmta.entity.type.MemberRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor; // Added AllArgsConstructor

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // Added AllArgsConstructor
@Builder // Moved Builder to class level
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
    @Builder.Default // Added Builder.Default
    private List<TeamMembers> teamMembersList = new ArrayList<>();
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Added Builder.Default
    private List<AvailableTime> availableTimeList = new ArrayList<>();
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Added Builder.Default
    private List<Vote> voteList = new ArrayList<>();
}