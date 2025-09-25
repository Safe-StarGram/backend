package com.github.controller;

import com.github.dto.CommentCreateRequest;
import com.github.entity.CommentEntity;
import com.github.jwt.JwtTokenProvider;
import com.github.service.CommentService;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/comment/{postId}")
    public ResponseEntity<List<CommentEntity>> getCommentsByPostId(@PathVariable Long postId) {
        System.out.println("=== 게시글별 댓글 조회 진행 ===");
        System.out.println("PostId: " + postId);

        List<CommentEntity> comments = commentService.getCommentsByPostId(postId);

        System.out.println("Found " + comments.size() + " comments");
        System.out.println("=== 게시글별 댓글 조회 완료 ===");
        
        return ResponseEntity.ok(comments);
    }

    @PostMapping("/comment")
    public ResponseEntity<CommentEntity> createComment(
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        System.out.println("=== 댓글 작성 진행 ===");
        
        String token = extractTokenFromRequest(httpRequest);
        Long userId = jwtTokenProvider.getUserId(token);
        
        // JWT에서 가져온 userId로 설정
        request.setUserId(userId);
        
        System.out.println("Request: " + request);

        CommentEntity comment = commentService.createComment(request);

        System.out.println("Comment created: " + comment);
        System.out.println("=== 댓글 작성 완료 ===");
        
        return ResponseEntity.ok(comment);
    }

    @PatchMapping("/comment/modify/{commentId}")
    public ResponseEntity<CommentEntity> updateComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        System.out.println("=== 댓글 수정 진행 ===");
        System.out.println("CommentId: " + commentId);

        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            throw new RuntimeException("댓글 내용은 필수입니다.");
        }

        CommentEntity comment = commentService.updateComment(commentId, message);

        System.out.println("Comment updated: " + comment);
        System.out.println("=== 댓글 수정 완료 ===");
        
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/comment/delete/{commentId}")
    public ResponseEntity<Map<String, String>> deleteComment(
            @PathVariable Long commentId,
            HttpServletRequest httpRequest
    ) {
        System.out.println("=== 댓글 삭제 진행 ===");
        System.out.println("CommentId: " + commentId);

        commentService.deleteComment(commentId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "댓글이 삭제되었습니다.");

        System.out.println("Comment deleted: " + commentId);
        System.out.println("=== 댓글 삭제 완료 ===");
        
        return ResponseEntity.ok(response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("토큰을 찾을 수 없습니다.");
    }
}

