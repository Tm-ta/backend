package com.example.tmta.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;

import java.util.List;

import static jakarta.persistence.GenerationType.*;

@Entity
@Getter
public class Team extends BaseEntity{
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "team")
    private List<TeamMembers> teamMembersList;

    private String TeamName;
    private String profileImage;
}
