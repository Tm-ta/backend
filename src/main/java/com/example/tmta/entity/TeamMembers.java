package com.example.tmta.entity;

import com.example.tmta.entity.type.TeamRole;
import com.example.tmta.entity.type.InviteState;
import jakarta.persistence.*;

import static jakarta.persistence.EnumType.*;
import static jakarta.persistence.GenerationType.*;

@Entity
public class TeamMembers extends BaseEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Team team;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(value = STRING)
    private InviteState inviteState;

    @Enumerated(value = STRING)
    private TeamRole teamRole;

}
