package com.github.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEntity {
    private Long postId;
    private Long subAreaId;
    private Long areaId;  // area_id 필드 추가
    private Long reporterId;
    
    // 신고자 정보
    private String reporterName;
    private Integer reporterPosition;     // 직책 ID (정수)
    private Integer reporterDepartment;   // 부서 ID (정수)
    
    // 확인자 정보
    private Long isCheckedId;       // 확인한 사람 ID (is_checked_id)
    private String checkerName;     // 확인한 사람 이름
    private Integer checkerPosition;      // 확인한 사람 직책 ID (정수)
    private Integer checkerDepartment;    // 확인한 사람 부서 ID (정수)
    
    // 조치자 정보 (현재 데이터베이스 스키마에 맞게)
    private Long actionTakerId;    // 조치한 사람 ID
    private String actionTakerName; // 조치한 사람 이름
    private Integer actionTakerPosition;  // 조치한 사람 직책 ID (정수)
    private Integer actionTakerDepartment; // 조치한 사람 부서 ID (정수)
    
    private String title;
    private String content;

    private String postPhotoUrl; // 이미지 URL

    // 신고자 위험성 평가 (분리된 형식)
    private Integer reporterRiskScore;      // 신고자 평가 점수 (1-5점)
    private String reporterRiskDescription; // 신고자 평가 설명
    private String reporterRisk;            // 기존 호환성 유지
    
    // 관리자 위험성 평가
    private String managerRisk;             // 1~5점 문자열로 저장
    private Integer isChecked;    // 0: 미확인, 1: 확인완료
    private Integer isActionTaken; // 0: 미조치, 1: 조치완료
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime isCheckedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime isActionTakenAt;
}