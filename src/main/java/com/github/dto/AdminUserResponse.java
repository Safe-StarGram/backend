package com.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private Integer userId;
    private String name;
    private String phoneNumber;
    private String radioNumber;
    private String department;
    private String position;
    private String profilePhotoUrl;
    private boolean hasAdminPermission; // 관리자 권한 여부
    private Integer role; // 사용자 역할 (1: 관리자, 0: 일반 사용자)
}
