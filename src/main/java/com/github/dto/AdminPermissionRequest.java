package com.github.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminPermissionRequest {
    @NotNull(message = "사용자 ID는 필수입니다.")
    private Integer userId; // 권한을 변경할 사용자 ID
    
    @NotNull(message = "권한 설정은 필수입니다.")
    private boolean grantPermission; // true: 권한 부여, false: 권한 제거
}
















