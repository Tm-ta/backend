package com.example.tmta.entity;

import com.example.tmta.entity.type.InviteState;
import com.example.tmta.entity.type.TeamRole;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
public class TeamMembers extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(value = STRING)
    private InviteState inviteState;

    @Enumerated(value = STRING)
    private TeamRole teamRole;

}
