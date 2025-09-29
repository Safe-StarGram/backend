package com.github.controller;

import com.github.dto.AdminPermissionRequest;
import com.github.dto.AdminUserResponse;
import com.github.dto.AdminPostUpdateRequest;
import com.github.entity.PostEntity;
import com.github.jwt.JwtTokenProvider;
import com.github.service.AdminService;
import com.github.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class BackofficeController {

    private final AdminService adminService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PostService postService;

    /**
     * 관리자 일람 - 모든 사용자 목록 조회
     * GET /api/admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getAllUsers(
            @RequestParam(required = false) String department,
            HttpServletRequest request
    ) {
        try {
            log.info("=== 관리자 일람 API 호출 ===");
            
            // JWT 토큰에서 사용자 정보 추출 (관리자 권한 확인용)
            String token = extractTokenFromRequest(request);
            Long currentUserId = jwtTokenProvider.getUserId(token);
            String currentUserRole = jwtTokenProvider.getRole(token);
            
            log.info("현재 사용자 ID: {}, 역할: {}", currentUserId, currentUserRole);
            
            // 사용자 목록 조회
            List<AdminUserResponse> users = adminService.getAllUsers(department);
            
            log.info("조회된 사용자 수: {}", users.size());
            log.info("=== 관리자 일람 API 완료 ===");
            
            return ResponseEntity.ok(users);
            
        } catch (Exception e) {
            log.error("관리자 일람 조회 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 사용자 권한 관리 - 권한 부여/제거
     * PUT /api/admin/permission
     */
    @PutMapping("/permission")
    public ResponseEntity<Map<String, Object>> updateUserPermission(
            @Valid @RequestBody AdminPermissionRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            log.info("=== 사용자 권한 변경 API 호출 ===");
            log.info("Target UserId: {}, GrantPermission: {}", request.getUserId(), request.isGrantPermission());
            
            // JWT 토큰에서 현재 사용자 정보 추출
            String token = extractTokenFromRequest(httpRequest);
            Long currentUserId = jwtTokenProvider.getUserId(token);
            String currentUserRole = jwtTokenProvider.getRole(token);
            
            log.info("현재 사용자 ID: {}, 역할: {}", currentUserId, currentUserRole);
            log.info("토큰에서 추출한 역할: '{}'", currentUserRole);
            log.info("ROLE_ADMIN과 비교: {}", "ROLE_ADMIN".equals(currentUserRole));
            
            // 자신의 권한을 변경하려는 경우 방지
            if (currentUserId.equals(request.getUserId().longValue())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "자신의 권한은 변경할 수 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 권한 변경 실행
            AdminUserResponse updatedUser = adminService.updateUserPermission(request.getUserId(), request.isGrantPermission());
            
            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("message", request.isGrantPermission() ? "관리자 권한이 부여되었습니다." : "관리자 권한이 제거되었습니다.");
            response.put("user", updatedUser);
            
            log.info("권한 변경 완료: {} -> {}", 
                    updatedUser.getName(), 
                    request.isGrantPermission() ? "관리자" : "일반사용자");
            log.info("=== 사용자 권한 변경 API 완료 ===");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자 권한 변경 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 특정 사용자 정보 조회
     * GET /api/admin/users/{userId}
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserResponse> getUserById(
            @PathVariable int userId,
            HttpServletRequest request
    ) {
        try {
            log.info("=== 사용자 상세 조회 API 호출 ===");
            log.info("Target UserId: {}", userId);
            
            // JWT 토큰에서 사용자 정보 추출
            String token = extractTokenFromRequest(request);
            Long currentUserId = jwtTokenProvider.getUserId(token);
            
            log.info("현재 사용자 ID: {}", currentUserId);
            
            // 사용자 목록에서 해당 사용자 찾기 (간단한 구현)
            List<AdminUserResponse> users = adminService.getAllUsers(null);
            AdminUserResponse targetUser = users.stream()
                    .filter(user -> user.getUserId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
            
            log.info("사용자 조회 완료: {}", targetUser.getName());
            log.info("=== 사용자 상세 조회 API 완료 ===");
            
            return ResponseEntity.ok(targetUser);
            
        } catch (Exception e) {
            log.error("사용자 상세 조회 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 사용자 삭제 (관리자 권한 필요)
     * DELETE /api/admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable int userId,
            HttpServletRequest request
    ) {
        try {
            log.info("=== 사용자 삭제 API 호출 ===");
            log.info("Target UserId: {}", userId);
            
            // JWT 토큰에서 현재 사용자 정보 추출
            String token = extractTokenFromRequest(request);
            Long currentUserId = jwtTokenProvider.getUserId(token);
            String currentUserRole = jwtTokenProvider.getRole(token);
            
            log.info("현재 사용자 ID: {}, 역할: {}", currentUserId, currentUserRole);
            
            // 관리자 권한 확인
            if (!"ROLE_ADMIN".equals(currentUserRole)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "관리자 권한이 필요합니다.");
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            // 자신을 삭제하려는 경우 방지
            if (currentUserId.equals((long) userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "자신의 계정은 삭제할 수 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 삭제 전 참조 데이터 정보 조회
            var refInfo = adminService.getUserReferenceInfo(userId);
            
            // 사용자 삭제 (참조 데이터도 함께 삭제)
            adminService.deleteUser(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "사용자가 삭제되었습니다.");
            response.put("deletedData", Map.of(
                "comments", refInfo.getCommentCount(),
                "posts", refInfo.getPostCount(),
                "checkedPosts", refInfo.getCheckedPostCount(),
                "actionPosts", refInfo.getActionPostCount()
            ));
            
            log.info("사용자 삭제 완료: userId={}", userId);
            log.info("=== 사용자 삭제 API 완료 ===");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자 삭제 중 오류 발생: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 사용자 삭제 전 참조 데이터 확인
     * GET /api/admin/users/{userId}/references
     */
    @GetMapping("/users/{userId}/references")
    public ResponseEntity<Map<String, Object>> getUserReferences(
            @PathVariable int userId,
            HttpServletRequest request
    ) {
        try {
            log.info("=== 사용자 참조 데이터 조회 API 호출 ===");
            log.info("Target UserId: {}", userId);
            
            // JWT 토큰에서 현재 사용자 정보 추출
            String token = extractTokenFromRequest(request);
            String currentUserRole = jwtTokenProvider.getRole(token);
            
            // 관리자 권한 확인
            if (!"ROLE_ADMIN".equals(currentUserRole)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "관리자 권한이 필요합니다.");
                return ResponseEntity.status(403).body(errorResponse);
            }
            
            var refInfo = adminService.getUserReferenceInfo(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("references", Map.of(
                "comments", refInfo.getCommentCount(),
                "posts", refInfo.getPostCount(),
                "checkedPosts", refInfo.getCheckedPostCount(),
                "actionPosts", refInfo.getActionPostCount(),
                "hasReferences", refInfo.hasReferences()
            ));
            
            log.info("사용자 참조 데이터 조회 완료: userId={}", userId);
            log.info("=== 사용자 참조 데이터 조회 API 완료 ===");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("사용자 참조 데이터 조회 중 오류 발생: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 관리자용 게시글 수정 (관리자 권한 필요)
     * PATCH /api/admin/notices/{postId}
     */
    @PatchMapping("/notices/{postId}")
    public ResponseEntity<PostEntity> updatePostByAdmin(
            @PathVariable Long postId,
            @Valid @RequestBody AdminPostUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            log.info("=== 관리자용 게시글 수정 API 호출 ===");
            log.info("PostId: {}, Request: {}", postId, request);
            
            // JWT 토큰에서 현재 사용자 정보 추출
            String token = extractTokenFromRequest(httpRequest);
            String currentUserRole = jwtTokenProvider.getRole(token);
            
            log.info("현재 사용자 역할: {}", currentUserRole);
            
            // 관리자 권한 확인
            if (!"ROLE_ADMIN".equals(currentUserRole)) {
                throw new RuntimeException("관리자 권한이 필요합니다.");
            }
            
            // 관리자용 게시글 수정 실행
            PostEntity updatedPost = postService.updatePostByAdmin(postId, request, token);
            
            log.info("게시글 수정 완료: postId={}", postId);
            log.info("=== 관리자용 게시글 수정 API 완료 ===");
            
            return ResponseEntity.ok(updatedPost);
            
        } catch (Exception e) {
            log.error("관리자용 게시글 수정 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("게시글 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * JWT 토큰을 요청에서 추출합니다
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            // 토큰 유효성 검증
            if (!jwtTokenProvider.validate(token)) {
                throw new RuntimeException("유효하지 않은 토큰입니다.");
            }
            return token;
        }
        throw new RuntimeException("토큰을 찾을 수 없습니다.");
    }
}
