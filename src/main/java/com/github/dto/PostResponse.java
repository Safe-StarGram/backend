package com.github.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponse {
    private Long postId;
    private String title;
    private String postPhotoUrl;
    private String content;
    
    private Long areaId;
    private Long subAreaId;
    private Long reporterId;
    
    private Integer isChecked;
    private Long checkerId;
    private LocalDateTime checkedAt;
    
    private Integer isActionTaked;
    private Long actionTakerId;
    private LocalDateTime actionTakenAt;
    
    private Integer reporterRiskScore;
    private String reporterRiskDescription;
    private String reporterRisk;
    private String managerRisk;
    
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime updatedAt;
}

