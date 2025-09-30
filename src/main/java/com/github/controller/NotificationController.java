package com.github.controller;

import com.github.entity.PostEntity;
import com.github.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<PostEntity>> getRecentRiskReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        System.out.println("=== NotificationController.getRecentRiskReports() 시작 ===");
        System.out.println("Page: " + page + ", Size: " + size);

        List<PostEntity> riskReports = notificationService.getRecentRiskReports(page, size);

        System.out.println("Returning " + riskReports.size() + " recent risk reports");
        System.out.println("=== NotificationController.getRecentRiskReports() 완료 ===");
        
        return ResponseEntity.ok(riskReports);
    }

    // 불필요한 기능들 제거: 읽음 상태, 알림 카운트 등
    // 실제로는 위험보고 목록 조회만 필요하므로 단순화
}

