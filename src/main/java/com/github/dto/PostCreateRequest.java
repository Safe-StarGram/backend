package com.github.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {
    
    // userId는 JWT 토큰에서 자동으로 설정되므로 validation 제거
    private Long userId;
    
    @NotNull(message = "구역 ID는 필수입니다.")
    private Long areaId;
    
    @NotNull(message = "세부 구역 ID는 필수입니다.")
    private Long subAreaId;
    
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;
    
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 1000, message = "내용은 1000자 이하여야 합니다.")
    private String content;
    
    @NotNull(message = "위험도 점수는 필수입니다.")
    @Min(value = 1, message = "위험도 점수는 1점 이상이어야 합니다.")
    @Max(value = 5, message = "위험도 점수는 5점 이하여야 합니다.")
    private Integer reporterRisk;
    
    // 신고자 부서/직책 (프론트엔드에서 정수로 전송)
    private String reporterDepartment;  // 문자열로 받아서 정수로 변환
    private String reporterPosition;    // 문자열로 받아서 정수로 변환
    
    // 점수를 문자열로 반환 (1~5)
    public String getReporterRisk() {
        return reporterRisk != null ? String.valueOf(reporterRisk) : null;
    }
    
    // 부서를 정수로 변환 (문자열로 와도 처리 가능)
    public Integer getReporterDepartmentAsInt() {
        if (reporterDepartment == null || reporterDepartment.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(reporterDepartment.trim());
        } catch (NumberFormatException e) {
            return null; // 변환 실패시 null 반환
        }
    }
    
    // 직책을 정수로 변환 (문자열로 와도 처리 가능)
    public Integer getReporterPositionAsInt() {
        if (reporterPosition == null || reporterPosition.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(reporterPosition.trim());
        } catch (NumberFormatException e) {
            return null; // 변환 실패시 null 반환
        }
    }
}