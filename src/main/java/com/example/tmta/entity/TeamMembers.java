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

    @Enumerated(value = STRING)
    private InviteState inviteState;

    @Enumerated(value = STRING)
    @Column(nullable = false)
    private TeamRole teamRole;

    @Column(length = 30)
    private String teamNickName;

    private String teamProfileImage;
    private boolean teamProfileSetupCompleted;

    @Builder
    public TeamMembers(Team team, Member member, InviteState inviteState, TeamRole teamRole,
                       String teamNickName, String teamProfileImage, boolean teamProfileSetupCompleted) {
        this.team = team;
        this.member = member;
        this.inviteState = inviteState;
        this.teamRole = teamRole;
        this.teamNickName = teamNickName;
        this.teamProfileImage = teamProfileImage;
        this.teamProfileSetupCompleted = teamProfileSetupCompleted;
    }

    public void updateTeamRole(TeamRole teamRole) {
        this.teamRole = teamRole;
    }

    public void updateTeamProfile(String teamNickName, String teamProfileImage) {
        this.teamNickName = teamNickName;
        this.teamProfileImage = teamProfileImage;
        this.teamProfileSetupCompleted = true;
    }
}
