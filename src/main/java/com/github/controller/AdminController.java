package com.github.controller;

import com.github.dto.AdminPermissionRequest;
import com.github.dto.AdminUserResponse;
import com.github.jwt.JwtTokenProvider;
import com.github.service.AdminService;
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
public class AdminController {

    private final AdminService adminService;
    private final JwtTokenProvider jwtTokenProvider;

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
     * PUT /api/admin/users/{userId}/permission
     */
    @PutMapping("/users/{userId}/permission")
    public ResponseEntity<Map<String, Object>> updateUserPermission(
            @PathVariable int userId,
            @Valid @RequestBody AdminPermissionRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            log.info("=== 사용자 권한 변경 API 호출 ===");
            log.info("Target UserId: {}, GrantPermission: {}", userId, request.isGrantPermission());
            
            // JWT 토큰에서 현재 사용자 정보 추출
            String token = extractTokenFromRequest(httpRequest);
            Long currentUserId = jwtTokenProvider.getUserId(token);
            String currentUserRole = jwtTokenProvider.getRole(token);
            
            log.info("현재 사용자 ID: {}, 역할: {}", currentUserId, currentUserRole);
            log.info("토큰에서 추출한 역할: '{}'", currentUserRole);
            log.info("ROLE_ADMIN과 비교: {}", "ROLE_ADMIN".equals(currentUserRole));
            
            // 자신의 권한을 변경하려는 경우 방지
            if (currentUserId.equals((long) userId)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "자신의 권한은 변경할 수 없습니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // 권한 변경 실행
            AdminUserResponse updatedUser = adminService.updateUserPermission(userId, request.isGrantPermission());
            
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
