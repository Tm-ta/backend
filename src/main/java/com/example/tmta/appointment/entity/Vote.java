package com.example.tmta.appointment.entity;
import com.example.tmta.common.entity.BaseEntity;
import com.example.tmta.member.entity.Member;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
@Getter
public class Vote extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "optional_time_id")
    private OptionalTime optionalTime;
}
