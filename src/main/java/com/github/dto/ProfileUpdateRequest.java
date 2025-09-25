package com.github.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {
    private String name;
    private String phoneNumber;
    private String radioNumber;
    private String profilePhotoUrl;
    private String department;
    private String position;
}
