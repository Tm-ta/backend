package com.example.tmta.entity;

import com.example.tmta.entity.type.InviteState;
import com.example.tmta.entity.type.TeamRole;
import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Setter
    @Enumerated(value = STRING)
    private InviteState inviteState;

    @Enumerated(value = STRING)
    @Column(nullable = false)
    private TeamRole teamRole;

    @Builder
    public TeamMembers(Team team, Member member, InviteState inviteState, TeamRole teamRole) {
        this.team = team;
        this.member = member;
        this.inviteState = inviteState;
        this.teamRole = teamRole;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
