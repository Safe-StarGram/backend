// src/main/java/com/example/area/repository/AreaJdbcRepository.java
package com.github.repository;

import com.github.entity.AreaEntity;
import com.github.entity.SubAreaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AreaJdbcRepository {
    private final JdbcTemplate jdbc;

    public Long insertArea(String name, String imageUrl) {
        String sql = "INSERT INTO area(name, area_photo_url) VALUES (?, ?)";
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, imageUrl);
            return ps;
        }, kh);
        return kh.getKey() == null ? null : kh.getKey().longValue();
    }

    public int updateImageUrl(Long areaId, String imageUrl) {
        String sql = "UPDATE area SET area_photo_url = ? WHERE area_id = ?";
        return jdbc.update(sql, imageUrl, areaId);
    }


    public void insertSubAreas(Long areaId, List<String> names) {
        if (names == null || names.isEmpty()) return;
        String sql = "INSERT INTO sub_area(area_id, name) VALUES (?, ?)";
        jdbc.batchUpdate(sql, names, names.size(),
                (ps, name) -> {
                    ps.setLong(1, areaId);
                    ps.setString(2, name);
                });
    }

    // 관리구역 조회
    public Optional<AreaEntity> findArea(Long id) {
        String sql = "SELECT area_id, name, area_photo_url, created_at, updated_at FROM area WHERE area_id = ?";
        List<AreaEntity> list = jdbc.query(sql, (rs, n) ->
                        AreaEntity.builder()
                                .areaId(rs.getLong("area_id"))
                                .name(rs.getString("name"))
                                .imageUrl(rs.getString("area_photo_url"))
                                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                                .build()
                , id);

        return list.stream().findFirst();
    }

    // 소구역 조회
    public List<SubAreaEntity> findSubArea(Long areaId) {
        String sql = "SELECT sub_area_id, area_id, name, created_at, updated_at FROM sub_area WHERE area_id = ?";
        return jdbc.query(sql, (rs, n) ->
                        SubAreaEntity.builder()
                                .subAreaId(rs.getLong("sub_area_id"))
                                .areaId(rs.getLong("area_id"))
                                .name(rs.getString("name"))
                                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime()) // sub_area 테이블에 updated_at 컬럼 추가됨
                                .build()
                , areaId);
    }

    public void updateAreaName(Long areaId, String name) {
        String sql = "UPDATE area SET name = ? WHERE area_id = ?";
        jdbc.update(sql, name, areaId);
    }
    public void updateAreaImage(Long areaId, String imageUrl) {
        String sql = "UPDATE area SET area_photo_url = ? WHERE area_id = ?";
        jdbc.update(sql, imageUrl, areaId);
    }
    public void deleteSubAreas(Long areaId) {
        String sql = "DELETE FROM sub_area WHERE area_id = ?";
        jdbc.update(sql, areaId);
    }

    // 관리구역 일람 - 모든 관리구역 목록 조회
    public List<AreaEntity> findAllArea() {
        String sql = "SELECT area_id, name, area_photo_url, created_at, updated_at FROM area ORDER BY area_id ASC";
        return jdbc.query(sql, (rs, n) ->
                AreaEntity.builder()
                        .areaId(rs.getLong("area_id"))
                        .name(rs.getString("name"))
                        .imageUrl(rs.getString("area_photo_url"))
                        .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                        .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                        .build()
        );
    }

    public void deleteSubAreasByIds(List<Long> subAreaIds) {
        if (subAreaIds == null || subAreaIds.isEmpty()) {
            return;
        }
        String inSql = String.join(",", Collections.nCopies(subAreaIds.size(), "?"));
        String sql = String.format("DELETE FROM sub_area WHERE sub_area_id IN (%s)", inSql);
        jdbc.update(sql, subAreaIds.toArray());
    }

    /**
     * Area 삭제 (계단식 삭제)
     * 1. 해당 area의 모든 post 삭제
     * 2. 해당 area의 모든 sub_area 삭제  
     * 3. area 삭제
     */
    @Transactional
    public void deleteArea(Long areaId) {
        // 1. 해당 area를 참조하는 모든 post 삭제
        String deletePostsSql = "DELETE FROM post WHERE area_id = ?";
        int deletedPosts = jdbc.update(deletePostsSql, areaId);
        System.out.println("Deleted " + deletedPosts + " posts for area " + areaId);
        
        // 2. 해당 area의 모든 sub_area 삭제
        String deleteSubAreasSql = "DELETE FROM sub_area WHERE area_id = ?";
        int deletedSubAreas = jdbc.update(deleteSubAreasSql, areaId);
        System.out.println("Deleted " + deletedSubAreas + " sub_areas for area " + areaId);
        
        // 3. area 삭제
        String deleteAreaSql = "DELETE FROM area WHERE area_id = ?";
        int deletedArea = jdbc.update(deleteAreaSql, areaId);
        System.out.println("Deleted " + deletedArea + " area with id " + areaId);
        
        if (deletedArea == 0) {
            throw new RuntimeException("Area를 찾을 수 없습니다: " + areaId);
        }
    }

    /**
     * Area 삭제 전 참조 데이터 확인
     */
    public boolean hasReferences(Long areaId) {
        // post 테이블에서 참조 확인
        String checkPostsSql = "SELECT COUNT(*) FROM post WHERE area_id = ?";
        Integer postCount = jdbc.queryForObject(checkPostsSql, Integer.class, areaId);
        
        // sub_area 테이블에서 참조 확인
        String checkSubAreasSql = "SELECT COUNT(*) FROM sub_area WHERE area_id = ?";
        Integer subAreaCount = jdbc.queryForObject(checkSubAreasSql, Integer.class, areaId);
        
        return (postCount != null && postCount > 0) || (subAreaCount != null && subAreaCount > 0);
    }

    /**
     * Area 삭제 전 참조 데이터 개수 조회
     */
    public AreaReferenceInfo getReferenceInfo(Long areaId) {
        String checkPostsSql = "SELECT COUNT(*) FROM post WHERE area_id = ?";
        Integer postCount = jdbc.queryForObject(checkPostsSql, Integer.class, areaId);
        
        String checkSubAreasSql = "SELECT COUNT(*) FROM sub_area WHERE area_id = ?";
        Integer subAreaCount = jdbc.queryForObject(checkSubAreasSql, Integer.class, areaId);
        
        return new AreaReferenceInfo(
            postCount != null ? postCount : 0,
            subAreaCount != null ? subAreaCount : 0
        );
    }

    /**
     * 참조 정보를 담는 내부 클래스
     */
    public static class AreaReferenceInfo {
        private final int postCount;
        private final int subAreaCount;
        
        public AreaReferenceInfo(int postCount, int subAreaCount) {
            this.postCount = postCount;
            this.subAreaCount = subAreaCount;
        }
        
        public int getPostCount() { return postCount; }
        public int getSubAreaCount() { return subAreaCount; }
        public boolean hasReferences() { return postCount > 0 || subAreaCount > 0; }
    }
}

