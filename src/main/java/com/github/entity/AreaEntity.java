package com.github.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class AreaEntity {
    private Long areaId;
    private String name;

    private String imageUrl;


    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime updatedAt;
}
