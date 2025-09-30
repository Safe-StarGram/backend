package com.github.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class SubAreaEntity {
    private Long subAreaId;
    private Long areaId;
    private String name;
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
