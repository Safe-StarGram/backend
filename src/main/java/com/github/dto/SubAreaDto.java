package com.github.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@Builder
public class SubAreaDto {

    private Long subAreaId;

    private String name;

    public SubAreaDto(Long subAreaId, String name) {
        this.subAreaId = subAreaId;
        this.name = name;
    }

}

