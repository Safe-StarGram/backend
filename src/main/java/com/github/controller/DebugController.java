package com.github.controller;

import com.github.repository.PostJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final JdbcTemplate jdbcTemplate;
    private final PostJdbcRepository postJdbcRepository;

    @GetMapping("/posts")
    public Map<String, Object> debugPosts() {
        try {
            // 단순 카운트
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post", Integer.class);
            
            // 단순 조회
            List<Map<String, Object>> posts = jdbcTemplate.queryForList(
                "SELECT post_id, title, content FROM post ORDER BY post_id DESC LIMIT 5"
            );
            
            return Map.of(
                "count", count,
                "posts", posts,
                "status", "success"
            );
        } catch (Exception e) {
            return Map.of(
                "error", e.getMessage(),
                "status", "error"
            );
        }
    }
    
    @GetMapping("/create-tables")
    public Map<String, Object> createTables() {
        try {
            // PostJdbcRepository의 createTableIfNotExists 메서드 호출
            postJdbcRepository.insert(com.github.entity.PostEntity.builder()
                .subAreaId(1L)
                .areaId(1L)
                .reporterId(1L)
                .title("테스트용 더미 데이터")
                .content("테이블 생성 테스트")
                .reporterRisk("1")
                .postPhotoUrl(null)
                .isChecked(0)
                .isActionTaken(0)
                .build());
            
            return Map.of("status", "success", "message", "테이블이 생성되었습니다");
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
    
    @GetMapping("/check-area-table")
    public Map<String, Object> checkAreaTable() {
        try {
            // area 테이블 구조 확인
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "DESCRIBE area"
            );
            
            return Map.of(
                "status", "success", 
                "columns", columns
            );
        } catch (Exception e) {
            return Map.of(
                "status", "error", 
                "message", e.getMessage()
            );
        }
    }
}






