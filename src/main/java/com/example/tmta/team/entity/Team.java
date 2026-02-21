package com.example.tmta.team.entity;
import com.example.tmta.common.entity.BaseEntity;

import com.example.tmta.team.entity.type.NamePolicy;
import com.example.tmta.team.entity.type.PostPermission;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.util.UUID;

@Entity
@Table(indexes = {
        @Index(name = "idx_team_name", columnList = "name")
})
@Getter
@DynamicInsert
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Team extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 20, nullable = false)
    private String name;

    private String profileImage;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'LEADER_ONLY'")
    private PostPermission postPermission;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'USE_NICKNAME'")
    private NamePolicy namePolicy;

    @Version
    private Long version;

    public static Team create(String name, NamePolicy namePolicy, PostPermission postPermission) {
        return Team.builder()
                .name(name)
                .namePolicy(namePolicy)
                .postPermission(postPermission)
                .build();
    }
}
