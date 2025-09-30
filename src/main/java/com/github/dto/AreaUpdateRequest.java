package com.github.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaUpdateRequest {
    private String areaName;              // 관리구역 이름
    private List<String> subAreaNames;    // 소구역 이름 리스트 (새로 갈아끼우기)
}
