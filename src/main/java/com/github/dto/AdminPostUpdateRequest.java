package com.github.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPostUpdateRequest {
    @NotNull(message = "확인 상태는 필수입니다.")
    private Integer isChecked;    // 0: 미확인, 1: 확인완료
    
    private Long checkerId;       // 확인한 사람 ID
    
    private LocalDateTime checkedAt;  // 확인 시간
    
    @NotNull(message = "조치 상태는 필수입니다.")
    private Integer isActionTaked; // 0: 미조치, 1: 조치완료
    
    private Long actionTakerId;   // 조치한 사람 ID
    
    private LocalDateTime actionTakenAt;  // 조치 시간
    
    private String managerRisk;   // 관리자 위험도 평가 (1~5점 문자열)
}

