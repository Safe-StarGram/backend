package com.github.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AreaResponse {


    private Long id;

    private String areaName;

    private String imageUrl;

    private List<SubAreaDto> subAreas;

}



