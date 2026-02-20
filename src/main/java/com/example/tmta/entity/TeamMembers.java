package com.example.tmta.entity;

import com.example.tmta.entity.type.InviteState;
import com.example.tmta.entity.type.TeamRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.*;

import static jakarta.persistence.EnumType.STRING;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_team_member", columnNames = {"team_id", "member_id"})
        },
        indexes = {
                @Index(name = "idx_team_members_team", columnList = "team_id"),
                @Index(name = "idx_team_members_member", columnList = "member_id"),
                @Index(name = "idx_team_members_invite_state", columnList = "invite_state"),
                @Index(name = "idx_team_members_team_role", columnList = "team_role")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMembers extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private java.util.UUID teamId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(value = STRING)
    @Column(name = "invite_state", nullable = false)
    private InviteState inviteState;

    @Enumerated(value = STRING)
    @Column(name = "team_role", nullable = false)
    private TeamRole teamRole;

    @Column(length = 30)
    private String teamNickName;

    private String teamProfileImage;
    private boolean teamProfileSetupCompleted;

    @Version
    private Long version;

    @Builder
    public TeamMembers(java.util.UUID teamId, Long memberId, InviteState inviteState, TeamRole teamRole,
                       String teamNickName, String teamProfileImage, boolean teamProfileSetupCompleted) {
        this.teamId = teamId;
        this.memberId = memberId;
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
