package com.github.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEntity {
    private Long commentId;
    private Long postId;
    private Long userId;
    private String userName;
    private Integer positionId;
    private Integer departmentId;
    private String message;
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd/HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // 사용자 정보 추가 (응답 시에만 사용)
    private String profilePhotoUrl;
    
}

