package com.github.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private long   expiresIn;
    private Long   userId;
    private String role;  // 사용자 역할 정보 추가
}
