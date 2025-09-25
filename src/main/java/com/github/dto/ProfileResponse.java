package com.github.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private Integer userId;
    private String  name;
    private String  phoneNumber;
    private String  radioNumber;
    private String  profilePhotoUrl;
    private Integer department;
    private Integer position;
}

