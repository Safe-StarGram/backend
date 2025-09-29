package com.github.controller;

import com.github.dto.UserInfoResponse;
import com.github.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 사용자 정보 조회
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> getUserInfo(@PathVariable Integer userId) {
        try {
            log.info("=== 사용자 정보 조회 API 호출 ===");
            log.info("UserId: {}", userId);
            
            UserInfoResponse userInfo = userService.getUserInfo(userId);
            
            log.info("사용자 정보 조회 완료: userId={}, name={}", userId, userInfo.getName());
            log.info("=== 사용자 정보 조회 API 완료 ===");
            
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

