package com.example.tmta.dto;

import lombok.Data;

@Data
public class MemberInfo {
	private String name;
	private String id;
	private String profileImageUrl;

	public static MemberInfo of(Long memberId, String displayName, String profileImageUrl) {
		MemberInfo info = new MemberInfo();
		info.setId(String.valueOf(memberId));
		info.setName(displayName);
		info.setProfileImageUrl(profileImageUrl);
		return info;
	}
}
