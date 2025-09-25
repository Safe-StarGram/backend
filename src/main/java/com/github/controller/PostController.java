package com.github.controller;

import com.github.dto.PostCreateRequest;
import com.github.dto.ManagerRiskAssessmentRequest;
import com.github.entity.PostEntity;
import com.github.jwt.JwtTokenProvider;
import com.github.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.HashMap;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostEntity> create(
            HttpServletRequest request,
            @Valid @ModelAttribute PostCreateRequest req,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            System.out.println("=== 게시글 작성 진행 ===");
            System.out.println("Request received: " + req);
            System.out.println("Reporter Risk Score: " + req.getReporterRisk());
            
            String token = extractTokenFromRequest(request);
            System.out.println("Token extracted: " + token);
            
            Long userId = jwtTokenProvider.getUserId(token);
            System.out.println("User ID from token: " + userId);
            
            req.setUserId(userId); // JWT에서 userId 설정
            System.out.println("Request after setting userId: " + req);
            
            PostEntity createdPost = postService.create(req, image);
            System.out.println("Post created successfully: " + createdPost);
            
            return ResponseEntity.ok(createdPost);
        } catch (Exception e) {
            System.out.println("Error in create method: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new RuntimeException("토큰을 찾을 수 없습니다.");
    }

    @GetMapping("/posts")
    public ResponseEntity<List<PostEntity>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long subAreaId
    ) {
        System.out.println("=== 게시글 조회 진행 ===");
        System.out.println("Page: " + page + ", Size: " + size + ", SubAreaId: " + subAreaId);
        
        List<PostEntity> posts;
        if (subAreaId != null) {
            posts = postService.getPostsBySubArea(subAreaId, page, size);
        } else {
            posts = postService.getAllPosts(page, size);
        }
        
        System.out.println("Returning " + posts.size() + " posts");
        System.out.println("=== 게시글 조회 완료 ===");
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/posts/detail/{postId}")
    public ResponseEntity<PostEntity> getPostById(@PathVariable Long postId) {
        System.out.println("=== 게시글 상세 조회 진행 ===");
        System.out.println("PostId: " + postId);
        
        PostEntity post = postService.getPostById(postId);
        
        System.out.println("Post found: " + post.getTitle());
        System.out.println("=== 게시글 상세 조회 완료 ===");
        return ResponseEntity.ok(post);
    }

    

    // JSON 본문으로 게시글 수정 지원 (raw JSON)
    @PatchMapping("/posts/{postId}")
    public ResponseEntity<PostEntity> updatePost(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> updates,
            HttpServletRequest request
    ) {
        String token = extractTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserId(token);

        // JSON으로 전달된 키들은 서비스에서 그대로 매핑/변환 처리됨
        PostEntity updatedPost = postService.updatePost(postId, updates, userId);
        return ResponseEntity.ok(updatedPost);
    }


    @DeleteMapping("/posts/delete/{postId}")
    public ResponseEntity<Map<String, String>> deletePost(
            @PathVariable Long postId,
            HttpServletRequest request
    ) {
        String token = extractTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserId(token);
        
        postService.deletePost(postId, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "게시물이 삭제되었습니다.");
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/posts/action-status/{postId}")
    public ResponseEntity<PostEntity> updateActionStatus(
            @PathVariable Long postId,
            @RequestBody Map<String, Object> updates,
            HttpServletRequest request
    ) {
        System.out.println("=== 액션 상태 업데이트 진행 ===");
        System.out.println("PostId: " + postId + ", Updates: " + updates);
        
        String token = extractTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserId(token);
        
        PostEntity updatedPost = postService.updatePost(postId, updates, userId);
        
        System.out.println("Action status updated: " + updatedPost);
        System.out.println("=== 액션 상태 업데이트 완료 ===");
        
        return ResponseEntity.ok(updatedPost);
    }

    @PatchMapping("/posts/manager-risk/{postId}")
    public ResponseEntity<PostEntity> updateManagerRisk(
            @PathVariable Long postId,
            @Valid @RequestBody ManagerRiskAssessmentRequest assessment,
            HttpServletRequest request
    ) {
        System.out.println("=== 관리자 위험성 평가 업데이트 진행 ===");
        System.out.println("PostId: " + postId);
        System.out.println("Manager Risk: " + assessment.getManager_risk());
        System.out.println("Risk Level: " + assessment.getRiskLevel());
        
        String token = extractTokenFromRequest(request);
        Long userId = jwtTokenProvider.getUserId(token);
        String userRole = jwtTokenProvider.getRole(token);
        
        // 관리자 권한 확인
        if (!"ROLE_ADMIN".equals(userRole)) {
            System.out.println("권한 없음: " + userRole);
            return ResponseEntity.status(403).build(); // 403 Forbidden
        }
        
        // 점수만 저장 (1~5)
        Map<String, Object> managerRiskUpdate = new HashMap<>();
        managerRiskUpdate.put("manager_risk", String.valueOf(assessment.getManager_risk())); // 점수를 문자열로 저장
        
        PostEntity updatedPost = postService.updatePost(postId, managerRiskUpdate, userId);
        
        System.out.println("Manager risk updated: " + updatedPost.getManagerRisk());
        System.out.println("=== 관리자 위험성 평가 업데이트 완료 ===");
        
        return ResponseEntity.ok(updatedPost);
    }

}
