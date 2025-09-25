package com.github.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AreaCreateRequest {
    private String areaName;               // "구역"
    private List<String> subAreaNames;     // "소구역"들
}