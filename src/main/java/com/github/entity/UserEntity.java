package com.github.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    private Integer userId;
    private Integer areaId;
    private String name;
    private String email;
    private String password;
    private String profilePhotoUrl;
    private String phoneNumber; //varchar여서 int -> string으로 수정
    private String radioNumber; //varchar여서 int -> string으로 수정
    private String department;
    private String position;
    private Integer role;
    private String departmentName;  // 부서명
    private String positionName;    // 직책명
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime updatedAt;
}
