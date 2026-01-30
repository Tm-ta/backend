package com.example.tmta.entity;

import com.example.tmta.entity.type.NamePolicy;
import com.example.tmta.entity.type.PostPermission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMembers> teamMembersList = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appointment> appointmentList = new ArrayList<>();

    @Column(length = 20, nullable = false)
    private String name;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'LEADER_ONLY'")
    private PostPermission postPermission;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'USE_NICKNAME'")
    private NamePolicy namePolicy;

    @Builder
    public Team(String name, String profileImage, PostPermission postPermission, NamePolicy namePolicy) {
        this.name = name;
        this.profileImage = profileImage;
        this.postPermission = postPermission;
        this.namePolicy = namePolicy;
    }

    public void addTeamMember(TeamMembers teamMembers) {
        teamMembersList.add(teamMembers);
        teamMembers.setTeam(this);
    }
}
