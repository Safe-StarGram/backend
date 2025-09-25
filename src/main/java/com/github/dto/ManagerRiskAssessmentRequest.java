package com.github.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerRiskAssessmentRequest {
    
    @NotNull(message = "관리자 위험성 점수는 필수입니다.")
    @Min(value = 1, message = "관리자 위험성 점수는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "관리자 위험성 점수는 5점 이하여야 합니다.")
    private Integer manager_risk;
    
    // 점수에 따른 위험성 등급을 자동으로 반환
    public String getRiskLevel() {
        if (manager_risk == null) return null;
        
        switch (manager_risk) {
            case 1:
                return "1점 (매우 낮음)";
            case 2:
                return "2점 (낮음)";
            case 3:
                return "3점 (보통)";
            case 4:
                return "4점 (높음)";
            case 5:
                return "5점 (매우 높음)";
            default:
                return manager_risk + "점";
        }
    }
    
}


