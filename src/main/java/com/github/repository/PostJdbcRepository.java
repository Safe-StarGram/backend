package com.github.repository;

import com.github.entity.PostEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostJdbcRepository {

    private final JdbcTemplate jdbc;

    // 문자열을 Integer로 변환하는 헬퍼 메서드 (1 또는 0)
    private Integer convertStringToInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // 공통 RowMapper
    private final RowMapper<PostEntity> postRowMapper = (rs, rowNum) -> PostEntity.builder()
            .postId(rs.getLong("post_id"))
            .subAreaId(rs.getLong("sub_area_id"))
            .areaId(rs.getObject("area_id", Long.class))
            .reporterId(rs.getLong("reporter_id"))
            .checkerId(rs.getObject("checker_id", Long.class))
            .actionTakerId(rs.getObject("action_taker_id", Long.class))
            .title(rs.getString("title"))
            .content(rs.getString("content"))
            .reporterRisk(rs.getString("reporter_risk"))      // 1~5 숫자 문자열
            .managerRisk(rs.getString("manager_risk"))        // 1~5 숫자 문자열
            // 신고자 정보 (JOIN된 데이터)
            .reporterName(rs.getString("reporter_name"))
            .reporterPosition(rs.getObject("reporter_position", Integer.class))
            .reporterDepartment(rs.getObject("reporter_department", Integer.class))
            // 확인자 정보 (JOIN된 데이터)
            .checkerName(rs.getString("checker_name"))
            .checkerPosition(rs.getObject("checker_position", Integer.class))
            .checkerDepartment(rs.getObject("checker_department", Integer.class))
            // 조치자 정보 (JOIN된 데이터)
            .actionTakerName(rs.getString("action_taker_name"))
            .actionTakerPosition(rs.getObject("action_taker_position", Integer.class))
            .actionTakerDepartment(rs.getObject("action_taker_department", Integer.class))
            .isChecked(convertStringToInt(rs.getString("is_checked")))
            .isActionTaked(convertStringToInt(rs.getString("is_action_taken")))
            .postPhotoUrl(rs.getString("post_photo_url"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .checkedAt(rs.getTimestamp("checked_at") != null ? rs.getTimestamp("checked_at").toLocalDateTime() : null)
            .actionTakenAt(rs.getTimestamp("action_taken_at") != null ? rs.getTimestamp("action_taken_at").toLocalDateTime() : null)
            .build();

    // 간단한 RowMapper (필수 필드만)
    private final RowMapper<PostEntity> simplePostRowMapper = (rs, rowNum) -> PostEntity.builder()
            .postId(rs.getLong("post_id"))
            .subAreaId(rs.getLong("sub_area_id"))
            .reporterId(rs.getLong("reporter_id"))
            .title(rs.getString("title"))
            .content(rs.getString("content"))
            .reporterRisk(rs.getString("reporter_risk"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .build();

    public PostEntity insert(PostEntity post) {
        try {
            log.info("=== PostJdbcRepository.insert 시작 ===");
            log.info("Inserting post: {}", post);
            
            // 테이블이 존재하지 않으면 생성
            createTableIfNotExists();
            
            final String sql = """
                    INSERT INTO post (sub_area_id, area_id, reporter_id, title, content, reporter_risk, post_photo_url, is_checked, is_action_taken, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            log.info("SQL: {}", sql);
            log.info("Parameters: subAreaId={}, areaId={}, reporterId={}, title={}, content={}, reporterRisk={}", 
                    post.getSubAreaId(), post.getAreaId(), post.getReporterId(), post.getTitle(), post.getContent(), post.getReporterRisk());

            KeyHolder keyHolder = new GeneratedKeyHolder();

            int rowsAffected = jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setObject(1, post.getSubAreaId(), Types.BIGINT);
                ps.setObject(2, post.getAreaId(), Types.BIGINT);  // areaId 추가
                ps.setObject(3, post.getReporterId(), Types.BIGINT);
                ps.setString(4, post.getTitle());
                ps.setString(5, post.getContent());
                ps.setString(6, post.getReporterRisk());
                ps.setString(7, ""); // post_photo_url은 빈 문자열로 초기화
                ps.setString(8, post.getIsChecked() != null && post.getIsChecked() == 1 ? "1" : "0"); // 0 또는 1
                ps.setString(9, post.getIsActionTaked() != null && post.getIsActionTaked() == 1 ? "1" : "0"); // 0 또는 1
                ps.setTimestamp(10, new Timestamp(System.currentTimeMillis())); // 현재 시간 설정
                return ps;
            }, keyHolder);

            log.info("Rows affected: {}", rowsAffected);
            
            Long postId = keyHolder.getKey().longValue();
            post.setPostId(postId);
            
            log.info("Post inserted successfully with ID: {}", postId);
            log.info("=== PostJdbcRepository.insert 완료 ===");
            return post;
        } catch (Exception e) {
            log.error("Error in PostJdbcRepository.insert: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 개발용 테이블 생성 메서드 (필요시에만 사용)
    private void createTableIfNotExists() {
        final String createAreaTableSql = """
            CREATE TABLE IF NOT EXISTS area (
                area_id        INT AUTO_INCREMENT PRIMARY KEY,
                name           VARCHAR(100) NOT NULL,
                area_photo_url VARCHAR(255),
                created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;
            
        final String createSubAreaTableSql = """
            CREATE TABLE IF NOT EXISTS sub_area (
                sub_area_id INT AUTO_INCREMENT PRIMARY KEY,
                area_id     INT NOT NULL,
                name        VARCHAR(100) NOT NULL,
                created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            )
            """;
            
        final String createPostTableSql = """
            CREATE TABLE IF NOT EXISTS post (
                post_id            INT AUTO_INCREMENT PRIMARY KEY,
                sub_area_id        INT NOT NULL,
                area_id            INT,
                reporter_id        INT NOT NULL,  
                checker_id         INT,           
                action_taker_id    INT,           
                title              VARCHAR(200) NOT NULL,
                content            TEXT NOT NULL,
                reporter_risk      VARCHAR(50) NOT NULL,   
                manager_risk       VARCHAR(50),   
                is_checked         VARCHAR(50),
                is_action_taken    VARCHAR(50),
                post_photo_url     VARCHAR(255) NOT NULL,
                created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                checked_at         DATETIME NULL,
                action_taken_at    DATETIME NULL 
            )
            """;
            
        final String createPostPhotosTableSql = """
            CREATE TABLE IF NOT EXISTS post_photos (
                post_photos_id INT AUTO_INCREMENT PRIMARY KEY,
                post_id        INT NOT NULL,
                url            VARCHAR(255) NOT NULL,
                sort_order     INT NOT NULL DEFAULT 0
            )
            """;

        try {
            jdbc.execute(createAreaTableSql);
            jdbc.execute(createSubAreaTableSql);
            jdbc.execute(createPostTableSql);
            jdbc.execute(createPostPhotosTableSql);
            
            // 기존 테이블의 컬럼명 변경 (기존 데이터가 있는 경우)
            try {
                // is_checked_id를 checker_id로 변경
                jdbc.execute("ALTER TABLE post CHANGE COLUMN is_checked_id checker_id INT");
                log.info("Column is_checked_id renamed to checker_id");
            } catch (Exception e) {
                log.debug("Column is_checked_id does not exist or already renamed: {}", e.getMessage());
            }
            
            try {
                // is_checked_at을 checked_at으로 변경
                jdbc.execute("ALTER TABLE post CHANGE COLUMN is_checked_at checked_at DATETIME NULL");
                log.info("Column is_checked_at renamed to checked_at");
            } catch (Exception e) {
                log.debug("Column is_checked_at does not exist or already renamed: {}", e.getMessage());
            }
            
            try {
                // is_action_taken_at을 action_taken_at으로 변경
                jdbc.execute("ALTER TABLE post CHANGE COLUMN is_action_taken_at action_taken_at DATETIME NULL");
                log.info("Column is_action_taken_at renamed to action_taken_at");
            } catch (Exception e) {
                log.debug("Column is_action_taken_at does not exist or already renamed: {}", e.getMessage());
            }
            
            log.info("Tables created and columns renamed successfully");
        } catch (Exception e) {
            log.error("Failed to create tables or rename columns: {}", e.getMessage());
        }
    }

    public void insertPostPhoto(Long postId, String url, int sortOrder) {
        final String sql = "INSERT INTO post_photos (post_id, url, sort_order) VALUES (?, ?, ?)";
        jdbc.update(sql, postId, url, sortOrder);
        log.debug("Post photo inserted: postId={}, url={}, sortOrder={}", postId, url, sortOrder);
    }


    public PostEntity findById(Long postId) {
        // 사용자 정보와 JOIN하여 부서/직책 ID 가져오기
        final String sql = """
                SELECT p.post_id, p.sub_area_id, p.area_id, p.reporter_id, p.checker_id, p.action_taker_id, 
                       p.title, p.content, p.reporter_risk, p.manager_risk,
                       p.is_checked, p.is_action_taken, p.post_photo_url, p.created_at, p.updated_at, p.checked_at, p.action_taken_at,
                       ru.name as reporter_name, ru.department_id as reporter_department, ru.position_id as reporter_position,
                       cu.name as checker_name, cu.department_id as checker_department, cu.position_id as checker_position,
                       au.name as action_taker_name, au.department_id as action_taker_department, au.position_id as action_taker_position
                FROM post p
                LEFT JOIN users ru ON p.reporter_id = ru.users_id
                LEFT JOIN users cu ON p.checker_id = cu.users_id
                LEFT JOIN users au ON p.action_taker_id = au.users_id
                WHERE p.post_id = ?
                """;

        try {
            log.info("Finding post by id: {}", postId);
            PostEntity result = jdbc.queryForObject(sql, postRowMapper, postId);
            log.info("Post found: {}", result.getPostId());
            return result;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.warn("Post not found with ID: {}", postId);
            return null;
        } catch (Exception e) {
            log.error("Error finding post by ID: {}", postId, e);
            return null;
        }
    }

    public List<PostEntity> findAll(int page, int size) {
        // 먼저 전체 데이터 개수 확인
        try {
            Integer totalCount = jdbc.queryForObject("SELECT COUNT(*) FROM post", Integer.class);
            log.info("Total posts in database: {}", totalCount);
        } catch (Exception e) {
            log.error("Error counting posts", e);
        }

        final String sql = """
            SELECT p.post_id, p.sub_area_id, p.area_id, p.reporter_id, p.checker_id, p.action_taker_id, 
                   p.title, p.content, p.reporter_risk, p.manager_risk,
                   p.is_checked, p.is_action_taken, p.post_photo_url, p.created_at, p.updated_at, p.checked_at, p.action_taken_at,
                   ru.name as reporter_name, ru.department_id as reporter_department, ru.position_id as reporter_position,
                   cu.name as checker_name, cu.department_id as checker_department, cu.position_id as checker_position,
                   au.name as action_taker_name, au.department_id as action_taker_department, au.position_id as action_taker_position
            FROM post p
            LEFT JOIN users ru ON p.reporter_id = ru.users_id
            LEFT JOIN users cu ON p.checker_id = cu.users_id
            LEFT JOIN users au ON p.action_taker_id = au.users_id
            ORDER BY p.created_at DESC LIMIT ? OFFSET ?
            """;

        try {
            log.info("=== PostJdbcRepository.findAll() 시작 ===");
            log.info("SQL: {}", sql);
            log.info("Parameters: size={}, offset={}", size, page * size);
            
            List<PostEntity> result = jdbc.query(sql, postRowMapper, size, page * size);
            log.info("Found {} posts for page {} with size {}", result.size(), page, size);
            log.info("=== PostJdbcRepository.findAll() 완료 ===");
            return result;
        } catch (Exception e) {
            log.error("Error finding all posts", e);
            e.printStackTrace();
            return List.of();
        }
    }

    public List<PostEntity> findBySubArea(Long subAreaId, int page, int size) {
        final String sql = """
            SELECT post_id, sub_area_id, reporter_id, title, content, reporter_risk, created_at, updated_at 
            FROM post WHERE sub_area_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?
            """;

        try {
            List<PostEntity> result = jdbc.query(sql, simplePostRowMapper, subAreaId, size, page * size);
            return result;
        } catch (Exception e) {
            log.error("Error finding posts by subArea: {}", subAreaId, e);
            return List.of();
        }
    }

    public int countBySubArea(Long subAreaId) {
        final String sql = "SELECT COUNT(*) FROM post WHERE sub_area_id = ?";
        try {
            Integer count = jdbc.queryForObject(sql, Integer.class, subAreaId);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Error counting posts by subArea: {}", subAreaId, e);
            return 0;
        }
    }

    public int countActionTakenBySubArea(Long subAreaId) {
        final String sql = "SELECT COUNT(*) FROM post WHERE sub_area_id = ? AND is_action_taken = 'Y'";
        try {
            Integer count = jdbc.queryForObject(sql, Integer.class, subAreaId);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Error counting action taken posts by subArea: {}", subAreaId, e);
            return 0;
        }
    }

    public PostEntity update(Long postId, Map<String, Object> updates) {
        StringBuilder sql = new StringBuilder("UPDATE post SET ");
        boolean first = true;

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            if (!first) {
                sql.append(", ");
            }
            sql.append(entry.getKey()).append(" = ?");
            first = false;
        }
        sql.append(" WHERE post_id = ?");

        try {
            Object[] params = new Object[updates.size() + 1];
            int index = 0;
            for (Object value : updates.values()) {
                // LocalDateTime을 Timestamp로 변환
                if (value instanceof java.time.LocalDateTime) {
                    params[index++] = Timestamp.valueOf((java.time.LocalDateTime) value);
                } else {
                    params[index++] = value;
                }
            }
            params[index] = postId;

            log.info("Update SQL: {}", sql.toString());
            log.info("Update params: {}", java.util.Arrays.toString(params));
            
            jdbc.update(sql.toString(), params);
            return findById(postId);
        } catch (Exception e) {
            log.error("Error updating post: {}", postId, e);
            return null;
        }
    }

    public void delete(Long postId) {
        final String sql = "DELETE FROM post WHERE post_id = ?";
        try {
            jdbc.update(sql, postId);
        } catch (Exception e) {
            log.error("Error deleting post: {}", postId, e);
        }
    }

    // 블록(= area) 단위로 기간 내 보고건수 집계
    public List<Map<String, Object>> countReportsByBlock(LocalDate from, LocalDate to) {
        final String sql = """
        SELECT a.area_id AS blockId,
               a.name    AS blockName,
               COUNT(p.post_id) AS reportCount
        FROM post p
        JOIN sub_area s ON p.sub_area_id = s.sub_area_id
        JOIN area a     ON s.area_id = a.area_id
        WHERE DATE(p.created_at) BETWEEN ? AND ?
        GROUP BY a.area_id, a.name
        ORDER BY a.area_id
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("blockId", rs.getLong("blockId"));
            map.put("blockName", rs.getString("blockName"));
            map.put("reportCount", rs.getInt("reportCount"));
            return map;
        }, from, to);
    }

    // 블록(= area) 단위로 기간 내 조치건수 집계
    public List<Map<String, Object>> countActionsByBlock(LocalDate from, LocalDate to) {
        final String sql = """
        SELECT a.area_id AS blockId,
               a.name    AS blockName,
               COUNT(p.post_id) AS actionCount
        FROM post p
        JOIN sub_area s ON p.sub_area_id = s.sub_area_id
        JOIN area a     ON s.area_id = a.area_id
        WHERE DATE(p.created_at) BETWEEN ? AND ?
          AND (
                p.is_action_taken = 1
             OR p.is_action_taken = '1'
             OR p.is_action_taken = 'Y'
          )
        GROUP BY a.area_id, a.name
        ORDER BY a.area_id
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("blockId", rs.getLong("blockId"));
            map.put("blockName", rs.getString("blockName"));
            map.put("actionCount", rs.getInt("actionCount"));
            return map;
        }, from, to);
    }

    // 월별(YYYY-MM) + 블록(= area) 기준 신고 건수 집계
    public List<Map<String, Object>> countMonthlyReportsByBlock(LocalDate from, LocalDate to) {
        final String sql = """
        SELECT DATE_FORMAT(p.created_at, '%Y-%m') AS yearMonth,
               a.area_id AS blockId,
               a.name    AS blockName,
               COUNT(p.post_id) AS reportCount
        FROM post p
        JOIN sub_area s ON p.sub_area_id = s.sub_area_id
        JOIN area a     ON s.area_id = a.area_id
        WHERE DATE(p.created_at) BETWEEN ? AND ?
        GROUP BY DATE_FORMAT(p.created_at, '%Y-%m'), a.area_id, a.name
        ORDER BY yearMonth, a.area_id
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("yearMonth", rs.getString("yearMonth"));   // e.g. "2025-03"
            map.put("blockId", rs.getLong("blockId"));
            map.put("blockName", rs.getString("blockName"));
            map.put("reportCount", rs.getInt("reportCount"));
            return map;
        }, from, to);
    }

    // 블록별 고위험성(3점 이상) 조치건수
    public List<Map<String, Object>> countHighRiskActions(LocalDate from, LocalDate to) {
        final String sql = """
        SELECT a.area_id AS blockId,
               a.name AS blockName,
               CAST(p.reporter_risk AS UNSIGNED) AS riskScore,
               COUNT(p.post_id) AS actionCount
        FROM post p
        JOIN sub_area s ON p.sub_area_id = s.sub_area_id
        JOIN area a ON s.area_id = a.area_id
        WHERE DATE(p.created_at) BETWEEN ? AND ?
          AND p.is_action_taken = 'Y'
          AND CAST(p.reporter_risk AS UNSIGNED) >= 3
        GROUP BY a.area_id, a.name, riskScore
        ORDER BY a.area_id, riskScore
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("blockId", rs.getLong("blockId"));
            map.put("blockName", rs.getString("blockName"));
            map.put("riskScore", rs.getInt("riskScore"));
            map.put("actionCount", rs.getInt("actionCount"));
            return map;
        }, from, to);
    }

    // 구역별 월별 조치건수 (5개월 단위)
    public List<Map<String, Object>> countMonthlyActionsByBlock(LocalDate from, LocalDate to) {
        final String sql = """
        SELECT DATE_FORMAT(p.created_at, '%Y-%m') AS yearMonth,
               a.area_id AS blockId,
               a.name    AS blockName,
               COUNT(p.post_id) AS actionCount
        FROM post p
        JOIN sub_area s ON p.sub_area_id = s.sub_area_id
        JOIN area a     ON s.area_id = a.area_id
        WHERE DATE(p.created_at) BETWEEN ? AND ?
          AND (
                p.is_action_taken = 1
             OR p.is_action_taken = '1'
             OR p.is_action_taken = 'Y'
          )
        GROUP BY DATE_FORMAT(p.created_at, '%Y-%m'), a.area_id, a.name
        ORDER BY yearMonth, a.area_id
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("yearMonth", rs.getString("yearMonth"));   // e.g. "2025-03"
            map.put("blockId", rs.getLong("blockId"));
            map.put("blockName", rs.getString("blockName"));
            map.put("actionCount", rs.getInt("actionCount"));
            return map;
        }, from, to);
    }

    // 구역별 월별 고위험성 조치건수 (5개월 단위)
    public List<Map<String, Object>> countMonthlyHighRiskActionsByBlock(LocalDate from, LocalDate to) {
        final String sql = """
        SELECT DATE_FORMAT(p.created_at, '%Y-%m') AS yearMonth,
               a.area_id AS blockId,
               a.name AS blockName,
               CAST(p.reporter_risk AS UNSIGNED) AS riskScore,
               COUNT(p.post_id) AS actionCount
        FROM post p
        JOIN sub_area s ON p.sub_area_id = s.sub_area_id
        JOIN area a ON s.area_id = a.area_id
        WHERE DATE(p.created_at) BETWEEN ? AND ?
          AND p.is_action_taken = 'Y'
          AND CAST(p.reporter_risk AS UNSIGNED) >= 3
        GROUP BY DATE_FORMAT(p.created_at, '%Y-%m'), a.area_id, a.name, riskScore
        ORDER BY yearMonth, a.area_id, riskScore
        """;

        return jdbc.query(sql, (rs, rowNum) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("yearMonth", rs.getString("yearMonth"));   // e.g. "2025-03"
            map.put("blockId", rs.getLong("blockId"));
            map.put("blockName", rs.getString("blockName"));
            map.put("riskScore", rs.getInt("riskScore"));
            map.put("actionCount", rs.getInt("actionCount"));
            return map;
        }, from, to);
    }

}