package com.github.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCreateRequest {
    
    @NotNull(message = "게시물 ID는 필수입니다.")
    private Long postId;
    
    // JWT에서 자동으로 추출되므로 선택적 필드
    private Long userId;
    
    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 500, message = "댓글 내용은 500자 이하여야 합니다.")
    private String message;
    
    // authorName 제거 - JWT에서 사용자 정보를 가져와서 자동 설정
    // 보안상 프론트엔드에서 작성자 이름을 조작할 수 없도록 함
}
