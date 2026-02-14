package com.example.tmta.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ProfileSetupRequest {

    @NotBlank
    @Size(max = 30)
    private String nickname;

    @Size(max = 255)
    private String profileImage;
}
