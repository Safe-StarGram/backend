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
}
