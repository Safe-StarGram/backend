package com.github.service;

import com.github.repository.PostJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final PostJdbcRepository postJdbcRepository;

    /** 블록별 보고건수 */
    public List<Map<String, Object>> getReportCounts(LocalDate from, LocalDate to) {
        // Repository는 DATE(p.created_at) BETWEEN ? AND ? 를 사용 (LocalDate 그대로 바인딩)
        return postJdbcRepository.countReportsByBlock(from, to);
    }

    /** 블록별 조치건수 */
    public List<Map<String, Object>> getActionCounts(LocalDate from, LocalDate to) {
        return postJdbcRepository.countActionsByBlock(from, to);
    }

    /** 블록별 월별 집계 */
    public List<Map<String, Object>> getMonthlyReportTrend(LocalDate from, LocalDate to) {
        return postJdbcRepository.countMonthlyReportsByBlock(from, to);
    }

    /** 블록별 고위험성(3점 이상) 조치건수 */
    public List<Map<String, Object>> getHighRiskActions(LocalDate from, LocalDate to) {
        return postJdbcRepository.countHighRiskActions(from, to);
    }
}