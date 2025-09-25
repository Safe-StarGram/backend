package com.github.controller;

import com.github.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 블록별 보고건수
     * GET /stats/hazards?from=2025-01-01&to=2025-12-31
     */
    @GetMapping("/hazards")
    public ResponseEntity<Map<String, Object>> hazards(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        var reportCounts = statisticsService.getReportCounts(from, to);
        Map<String, Object> resp = new HashMap<>();
        resp.put("reportCounts", reportCounts);
        return ResponseEntity.ok(resp);
    }

    /**
     * 블록별 조치건수 (is_action_taken = 1)
     * GET /stats/actions?from=2025-01-01&to=2025-12-31
     */
    @GetMapping("/actions")
    public ResponseEntity<Map<String, Object>> actions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        var actionCounts = statisticsService.getActionCounts(from, to);
        Map<String, Object> resp = new HashMap<>();
        resp.put("actionCounts", actionCounts);
        return ResponseEntity.ok(resp);
    }

    /**
     * 월별 신고 건수 추이 (블록별)
     * GET /stats/monthly-reports?from=2025-01-01&to=2025-12-31
     */
    @GetMapping("/monthly-reports")
    public ResponseEntity<Map<String, Object>> monthlyReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        var items = statisticsService.getMonthlyReportTrend(from, to);
        Map<String, Object> resp = new HashMap<>();
        resp.put("monthlyReports", items);
        return ResponseEntity.ok(resp);
    }

    /**
     * 블록별 고위험성(3점 이상) 조치건수
     * GET /stats/high-risk-actions?from=2025-01-01&to=2025-12-31
     */
    @GetMapping("/high-risk-actions")
    public ResponseEntity<Map<String, Object>> highRiskActions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        var highRiskActions = statisticsService.getHighRiskActions(from, to);
        Map<String, Object> resp = new HashMap<>();
        resp.put("highRiskActions", highRiskActions);
        return ResponseEntity.ok(resp);
    }
}