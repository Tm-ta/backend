package com.example.tmta.entity;

import com.example.tmta.entity.type.MemberRole;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.GenerationType.*;

@Entity
@Getter
public class Member extends BaseEntity{
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private String name;
    private String nickName;
    private String email;
    private String authProvider;
    private String provider;
    private boolean pushAlarmAgree;
    @Enumerated(value = STRING)
    private MemberRole role;
    @OneToMany(mappedBy = "member")
    private List<TeamMembers> teamMembersList = new ArrayList<>();
    @OneToMany(mappedBy = "member")
    private List<AvailableTime> availableTimeList = new ArrayList<>();
}